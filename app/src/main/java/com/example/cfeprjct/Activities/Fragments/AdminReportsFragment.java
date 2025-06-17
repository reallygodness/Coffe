package com.example.cfeprjct.Activities.Fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.cfeprjct.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class AdminReportsFragment extends Fragment {
    private Spinner spinnerReportType;
    private Button btnGenerate;
    private TextView tvReportResult;

    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        spinnerReportType = v.findViewById(R.id.spinnerReportType);
        btnGenerate = v.findViewById(R.id.btnGenerateReport);
        tvReportResult = v.findViewById(R.id.tvReportResult);

        firestore = FirebaseFirestore.getInstance();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Выручка за месяц", "Количество клиентов за месяц", "Самый популярный товар"}
        );
        spinnerReportType.setAdapter(adapter);

        btnGenerate.setOnClickListener(view -> generateReport(spinnerReportType.getSelectedItemPosition()));

        return v;
    }

    private void generateReport(int reportType) {
        switch (reportType) {
            case 0:
                showRevenueWithProducts();
                break;
            case 1:
                showClientsWithNames();
                break;
            case 2:
                showMostPopularProducts();
                break;
        }
    }

    // --- Выручка за месяц с товарами ---
    private void showRevenueWithProducts() {
        long startOfMonth = getStartOfMonthTimestamp();
        firestore.collection("orders")
                .whereGreaterThan("createdAt", startOfMonth)
                .get()
                .addOnSuccessListener(ordersSnap -> {
                    double totalRevenue = 0;
                    Map<String, Integer> productQuantities = new HashMap<>();

                    List<DocumentSnapshot> ordersList = ordersSnap.getDocuments();
                    if (ordersList.isEmpty()) {
                        tvReportResult.setText("Нет заказов за этот месяц.");
                        return;
                    }

                    final int[] remain = {ordersList.size() * 3}; // drinks, dishes, desserts для каждого заказа
                    for (DocumentSnapshot orderDoc : ordersList) {
                        String orderId = orderDoc.getId();

                        // DRINKS
                        firestore.collection("orders").document(orderId)
                                .collection("drinks").get()
                                .addOnSuccessListener(drinksSnap -> {
                                    for (DocumentSnapshot drink : drinksSnap) {
                                        String key = "drink_" + drink.getLong("drinkId");
                                        int qty = drink.getLong("quantity") != null ? drink.getLong("quantity").intValue() : 0;
                                        productQuantities.put(key, productQuantities.getOrDefault(key, 0) + qty);
                                    }
                                    remain[0]--;
                                    if (remain[0] == 0)
                                        showRevenueResult(ordersList, productQuantities);
                                });
                        // DISHES
                        firestore.collection("orders").document(orderId)
                                .collection("dishes").get()
                                .addOnSuccessListener(dishesSnap -> {
                                    for (DocumentSnapshot dish : dishesSnap) {
                                        String key = "dish_" + dish.getLong("dishId");
                                        int qty = dish.getLong("quantity") != null ? dish.getLong("quantity").intValue() : 0;
                                        productQuantities.put(key, productQuantities.getOrDefault(key, 0) + qty);
                                    }
                                    remain[0]--;
                                    if (remain[0] == 0)
                                        showRevenueResult(ordersList, productQuantities);
                                });
                        // DESSERTS
                        firestore.collection("orders").document(orderId)
                                .collection("desserts").get()
                                .addOnSuccessListener(dessertsSnap -> {
                                    for (DocumentSnapshot dessert : dessertsSnap) {
                                        String key = "dessert_" + dessert.getLong("dessertId");
                                        int qty = dessert.getLong("quantity") != null ? dessert.getLong("quantity").intValue() : 0;
                                        productQuantities.put(key, productQuantities.getOrDefault(key, 0) + qty);
                                    }
                                    remain[0]--;
                                    if (remain[0] == 0)
                                        showRevenueResult(ordersList, productQuantities);
                                });
                    }
                });
    }

    private void showRevenueResult(List<DocumentSnapshot> ordersList, Map<String, Integer> productQuantities) {
        // Получаем цены из price_list и имена товаров
        double[] totalRevenue = {0};
        StringBuilder productsList = new StringBuilder();

        if (productQuantities.isEmpty()) {
            tvReportResult.setText("Нет товаров, повлиявших на выручку.");
            return;
        }

        final int[] remainProducts = {productQuantities.size()};
        for (String prodKey : productQuantities.keySet()) {
            String[] parts = prodKey.split("_");
            String type = parts[0];
            String id = parts[1];
            int quantity = productQuantities.get(prodKey);

            firestore.collection("price_list").document(type + "s")
                    .collection("items").document(id)
                    .get()
                    .addOnSuccessListener(priceDoc -> {
                        double unitPrice = priceDoc.contains("price") && priceDoc.getDouble("price") != null
                                ? priceDoc.getDouble("price") : 0;

                        firestore.collection(type + "s").document(id)
                                .get()
                                .addOnSuccessListener(prodDoc -> {
                                    String name = prodDoc.getString("name");
                                    // Накапливаем
                                    totalRevenue[0] += unitPrice * quantity;
                                    if (!TextUtils.isEmpty(name)) {
                                        productsList.append("- ")
                                                .append(name)
                                                .append(": ")
                                                .append(quantity)
                                                .append(" × ")
                                                .append((int) unitPrice)
                                                .append(" ₽ = ")
                                                .append((int) (quantity * unitPrice))
                                                .append(" ₽\n");
                                    }
                                    remainProducts[0]--;
                                    if (remainProducts[0] == 0) {
                                        tvReportResult.setText(
                                                "Выручка за месяц: " + ((int) totalRevenue[0]) + " ₽\n\n" +
                                                        (productsList.length() > 0
                                                                ? "Товары, повлиявшие на выручку:\n" + productsList
                                                                : "Нет заказанных товаров в этом месяце.")
                                        );
                                    }
                                });
                    });
        }
    }

    // --- Количество клиентов за месяц с ФИО ---
    private void showClientsWithNames() {
        long startOfMonth = getStartOfMonthTimestamp();
        firestore.collection("orders")
                .whereGreaterThan("createdAt", startOfMonth)
                .get()
                .addOnSuccessListener(ordersSnap -> {
                    Map<String, Boolean> clients = new HashMap<>();
                    for (QueryDocumentSnapshot doc : ordersSnap) {
                        String userId = doc.getString("userId");
                        if (!TextUtils.isEmpty(userId)) clients.put(userId, true);
                    }
                    if (clients.isEmpty()) {
                        tvReportResult.setText("Нет клиентов за этот месяц.");
                        return;
                    }
                    final int[] remain = {clients.size()};
                    StringBuilder sb = new StringBuilder();
                    for (String userId : clients.keySet()) {
                        firestore.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String fio = userDoc.getString("lastName") + " " +
                                            userDoc.getString("firstName");
                                    sb.append("- ").append(fio.trim()).append("\n");
                                    remain[0]--;
                                    if (remain[0] == 0) {
                                        tvReportResult.setText(
                                                "Клиентов за месяц: " + clients.size() + "\n" +
                                                        sb.toString()
                                        );
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    sb.append("- ").append(userId).append("\n");
                                    remain[0]--;
                                    if (remain[0] == 0) {
                                        tvReportResult.setText(
                                                "Клиентов за месяц: " + clients.size() + "\n" +
                                                        sb.toString()
                                        );
                                    }
                                });
                    }
                });
    }

    // --- ТОП-5 самых популярных товаров ---
    private void showMostPopularProducts() {
        firestore.collection("orders")
                .whereGreaterThan("createdAt", getStartOfMonthTimestamp())
                .get()
                .addOnSuccessListener(ordersSnap -> {
                    Map<String, Integer> productCounts = new HashMap<>();

                    List<DocumentSnapshot> ordersList = ordersSnap.getDocuments();
                    if (ordersList.isEmpty()) {
                        tvReportResult.setText("Нет заказов в этом месяце.");
                        return;
                    }

                    final int[] remain = {ordersList.size() * 3}; // 3 категории на каждый заказ
                    for (DocumentSnapshot orderDoc : ordersList) {
                        String orderId = orderDoc.getId();
                        // DRINKS
                        firestore.collection("orders").document(orderId)
                                .collection("drinks").get()
                                .addOnSuccessListener(drinksSnap -> {
                                    for (DocumentSnapshot drink : drinksSnap.getDocuments()) {
                                        String id = "drink_" + drink.getLong("drinkId");
                                        int qty = drink.getLong("quantity") != null ? drink.getLong("quantity").intValue() : 0;
                                        productCounts.put(id, productCounts.getOrDefault(id, 0) + qty);
                                    }
                                    remain[0]--;
                                    if (remain[0] == 0) showPopularProductsResult(productCounts);
                                });
                        // DISHES
                        firestore.collection("orders").document(orderId)
                                .collection("dishes").get()
                                .addOnSuccessListener(dishesSnap -> {
                                    for (DocumentSnapshot dish : dishesSnap.getDocuments()) {
                                        String id = "dish_" + dish.getLong("dishId");
                                        int qty = dish.getLong("quantity") != null ? dish.getLong("quantity").intValue() : 0;
                                        productCounts.put(id, productCounts.getOrDefault(id, 0) + qty);
                                    }
                                    remain[0]--;
                                    if (remain[0] == 0) showPopularProductsResult(productCounts);
                                });
                        // DESSERTS
                        firestore.collection("orders").document(orderId)
                                .collection("desserts").get()
                                .addOnSuccessListener(dessertsSnap -> {
                                    for (DocumentSnapshot dessert : dessertsSnap.getDocuments()) {
                                        String id = "dessert_" + dessert.getLong("dessertId");
                                        int qty = dessert.getLong("quantity") != null ? dessert.getLong("quantity").intValue() : 0;
                                        productCounts.put(id, productCounts.getOrDefault(id, 0) + qty);
                                    }
                                    remain[0]--;
                                    if (remain[0] == 0) showPopularProductsResult(productCounts);
                                });
                    }
                });
    }

    // Топ-5 популярных товаров
    private void showPopularProductsResult(Map<String, Integer> productCounts) {
        if (productCounts.isEmpty()) {
            tvReportResult.setText("Нет данных о популярных товарах.");
            return;
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(productCounts.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int top = Math.min(5, list.size());
        StringBuilder sb = new StringBuilder();
        sb.append("ТОП-5 самых популярных товаров за месяц:\n\n");

        // Счётчик для завершения всех асинхронных вызовов
        final int[] remain = {top};

        for (int i = 0; i < top; i++) {
            String prodKey = list.get(i).getKey();
            int count = list.get(i).getValue();

            // prodKey формата "drinks_2" или "dishes_3"
            String[] parts = prodKey.split("_");
            String category = parts[0];
            String id = parts[1];

            if ("dishes".equals(category + "s") && "2".equals(id)) {
                remain[0]--;
                if (remain[0] == 0) {
                    tvReportResult.setText(sb.toString());
                }
                continue;
            }

            // Категория во множественном числе в Firestore (drinks/dishes/desserts)
            String collectionName = category + "s";
            FirebaseFirestore.getInstance()
                    .collection(collectionName)
                    .document(id)
                    .get()
                    .addOnSuccessListener(productDoc -> {
                        String prodName = productDoc.getString("name");
                        if (prodName == null || prodName.trim().isEmpty()) {
                            prodName = category.substring(0, 1).toUpperCase() + category.substring(1) + " №" + id;
                        }
                        sb.append("- ").append(prodName).append(" (").append(count).append(" шт.)\n");
                        remain[0]--;
                        if (remain[0] == 0) {
                            tvReportResult.setText(sb.toString());
                        }
                    })
                    .addOnFailureListener(e -> {
                        sb.append("- ").append(category.substring(0, 1).toUpperCase())
                                .append(category.substring(1)).append(" №").append(id)
                                .append(" (").append(count).append(" шт.)\n");
                        remain[0]--;
                        if (remain[0] == 0) {
                            tvReportResult.setText(sb.toString());
                        }
                    });
        }
    }


    private long getStartOfMonthTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}