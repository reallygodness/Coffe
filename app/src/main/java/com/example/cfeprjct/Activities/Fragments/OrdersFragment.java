package com.example.cfeprjct.Activities.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cfeprjct.Adapters.OrderAdapter;
import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.AuthUtils;
import com.example.cfeprjct.DAOS.OrderDAO;
import com.example.cfeprjct.DAOS.OrderStatusDAO;
import com.example.cfeprjct.DAOS.OrderedDessertDAO;
import com.example.cfeprjct.DAOS.OrderedDishDAO;
import com.example.cfeprjct.DAOS.OrderedDrinkDAO;
import com.example.cfeprjct.Entities.Order;
import com.example.cfeprjct.Entities.OrderStatus;
import com.example.cfeprjct.Entities.OrderedDessert;
import com.example.cfeprjct.Entities.OrderedDish;
import com.example.cfeprjct.Entities.OrderedDrink;
import com.example.cfeprjct.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class OrdersFragment extends Fragment {

    private AppDatabase         db;
    private OrderDAO            orderDAO;
    private OrderedDrinkDAO     orderedDrinkDAO;
    private OrderedDishDAO      orderedDishDAO;
    private OrderedDessertDAO   orderedDessertDAO;
    private OrderStatusDAO      orderStatusDAO;
    private FirebaseFirestore   firestore;
    private String              userId;

    private RecyclerView        rvOrders;
    private OrderAdapter        orderAdapter;

    private TextView tvEmptyOrders;
    private List<OrderStatus> statuses;

    private static final String PREFS = "app_prefs";               // ← имя SharedPreferences
    private static final String KEY_LAST_USER = "lastUserId";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        // 1) Инициализация DAO и Firestore
        db                 = AppDatabase.getInstance(requireContext());
        orderDAO           = db.orderDAO();
        orderedDrinkDAO    = db.orderedDrinkDAO();
        orderedDishDAO     = db.orderedDishDAO();
        orderedDessertDAO  = db.orderedDessertDAO();
        orderStatusDAO     = db.orderStatusDAO();
        firestore          = FirebaseFirestore.getInstance();
        userId             = AuthUtils.getLoggedInUserId(requireContext());
        tvEmptyOrders = view.findViewById(R.id.tvEmptyOrders);

        rvOrders = view.findViewById(R.id.rvOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));

        // ← ИЗМЕНЕНИЕ: очистка локальных заказов при смене пользователя
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String lastUser = prefs.getString(KEY_LAST_USER, null);
        if (userId != null && !userId.equals(lastUser)) {
            Executors.newSingleThreadExecutor().execute(() -> {
                orderDAO.clearAll();
                orderedDrinkDAO.clearAll();
                orderedDishDAO.clearAll();
                orderedDessertDAO.clearAll();
            });
            prefs.edit().putString(KEY_LAST_USER, userId).apply();
        }
        // → конец изменения



        // 2) Выполняем в фоне: экспорт локальных → импорт облачных
        Executors.newSingleThreadExecutor().execute(() -> {
            // 2.1. Статусы
            statuses = orderStatusDAO.getAllStatuses();
            for (OrderStatus st : statuses) {
                Map<String,Object> sm = new HashMap<>();
                sm.put("statusId",   st.getStatusId());
                sm.put("statusName", st.getStatusName());
                firestore
                        .collection("order_statuses")
                        .document(String.valueOf(st.getStatusId()))
                        .set(sm);
            }

            // 2.2. Локальные заказы → выгрузка в Firestore
            List<Order> localOrders = orderDAO.getAllSync();
            for (Order o : localOrders) {
                String docId = String.valueOf(o.getOrderId());
                Map<String,Object> om = new HashMap<>();
                om.put("orderId",    o.getOrderId());
                om.put("userId",     o.getUserId());
                om.put("createdAt",  o.getCreatedAt());
                om.put("totalPrice", o.getTotalPrice());
                om.put("statusId",   o.getStatusId());
                firestore
                        .collection("orders")
                        .document(docId)
                        .set(om);

                // экспорт позиций
                exportOrderPositions(o.getOrderId());
            }

            // 2.3. Если локально нет ни одного — подгружаем из Firestore
            if (localOrders.isEmpty()) {
                firestore.collection("orders")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener(this::onOrdersFetchedFromCloud);
            }

            // 2.4. Настраиваем адаптер и LiveData
            requireActivity().runOnUiThread(() -> {
                orderAdapter = new OrderAdapter(requireContext(), statuses);
                rvOrders.setAdapter(orderAdapter);

                LiveData<List<Order>> live = orderDAO.getAllLiveOrders();
                live.observe(getViewLifecycleOwner(), orders -> {
                    orderAdapter.submitList(orders);
                    // показываем сообщение, если список пуст
                    tvEmptyOrders.setVisibility(
                            orders.isEmpty() ? View.VISIBLE : View.GONE
                    );
                });
            });
        });

        return view;
    }

    /** Выгружаем в Firestore позиции одного заказа */
    private void exportOrderPositions(int orderId) {
        String parent = String.valueOf(orderId);
        // напитки
        for (OrderedDrink od : orderedDrinkDAO.getByOrderId(orderId)) {
            Map<String,Object> m = new HashMap<>();
            m.put("orderedDrinkId", od.getOrderedDrinkId());
            m.put("orderId",        od.getOrderId());
            m.put("drinkId",        od.getDrinkId());
            m.put("quantity",       od.getQuantity());
            m.put("size",           od.getSize());
            firestore
                    .collection("orders")
                    .document(parent)
                    .collection("drinks")
                    .document(String.valueOf(od.getOrderedDrinkId()))
                    .set(m);
        }
        // блюда
        for (OrderedDish od : orderedDishDAO.getByOrderId(orderId)) {
            Map<String,Object> m = new HashMap<>();
            m.put("orderedDishId", od.getOrderedDishId());
            m.put("orderId",       od.getOrderId());
            m.put("dishId",        od.getDishId());
            m.put("quantity",      od.getQuantity());
            m.put("size",          od.getSize());
            firestore
                    .collection("orders")
                    .document(parent)
                    .collection("dishes")
                    .document(String.valueOf(od.getOrderedDishId()))
                    .set(m);
        }
        // десерты
        for (OrderedDessert od : orderedDessertDAO.getByOrderId(orderId)) {
            Map<String,Object> m = new HashMap<>();
            m.put("orderedDessertId", od.getOrderedDessertId());
            m.put("orderId",          od.getOrderId());
            m.put("dessertId",        od.getDessertId());
            m.put("quantity",         od.getQuantity());
            m.put("size",             od.getSize());
            firestore
                    .collection("orders")
                    .document(parent)
                    .collection("desserts")
                    .document(String.valueOf(od.getOrderedDessertId()))
                    .set(m);
        }
    }

    /** Колбэк: получили заказы из Firestore → сохраняем их и их позиции в Room (в фоне) */
    private void onOrdersFetchedFromCloud(QuerySnapshot snap) {
        Executors.newSingleThreadExecutor().execute(() -> {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Order o = new Order();
                o.setOrderId(doc.getLong("orderId").intValue());
                o.setUserId(    doc.getString("userId"));
                o.setCreatedAt( doc.getLong("createdAt"));
                o.setTotalPrice(doc.getDouble("totalPrice").floatValue());
                o.setStatusId(  doc.getLong("statusId").intValue());
                orderDAO.insertOrder(o);

                String orderDocId = doc.getId();
                // импорт позиций
                importOrderPositions(orderDocId);
            }
        });
    }

    private void importOrderPositions(String orderDocId) {
        // напитки
        firestore.collection("orders")
                .document(orderDocId)
                .collection("drinks")
                .get()
                .addOnSuccessListener(drinkSnap -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        for (DocumentSnapshot d : drinkSnap.getDocuments()) {
                            OrderedDrink od = new OrderedDrink();
                            od.setOrderedDrinkId(d.getLong("orderedDrinkId").intValue());
                            od.setOrderId(       d.getLong("orderId").intValue());
                            od.setDrinkId(       d.getLong("drinkId").intValue());
                            od.setQuantity(      d.getLong("quantity").intValue());
                            od.setSize(          d.getLong("size").intValue());
                            orderedDrinkDAO.insert(od);
                        }
                    });
                });
        // блюда
        firestore.collection("orders")
                .document(orderDocId)
                .collection("dishes")
                .get()
                .addOnSuccessListener(dishSnap -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        for (DocumentSnapshot d : dishSnap.getDocuments()) {
                            OrderedDish od = new OrderedDish();
                            od.setOrderedDishId(d.getLong("orderedDishId").intValue());
                            od.setOrderId(      d.getLong("orderId").intValue());
                            od.setDishId(       d.getLong("dishId").intValue());
                            od.setQuantity(     d.getLong("quantity").intValue());
                            od.setSize(         d.getLong("size").intValue());
                            orderedDishDAO.insert(od);
                        }
                    });
                });
        // десерты
        firestore.collection("orders")
                .document(orderDocId)
                .collection("desserts")
                .get()
                .addOnSuccessListener(desSnap -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        for (DocumentSnapshot d : desSnap.getDocuments()) {
                            OrderedDessert od = new OrderedDessert();
                            od.setOrderedDessertId(d.getLong("orderedDessertId").intValue());
                            od.setOrderId(         d.getLong("orderId").intValue());
                            od.setDessertId(       d.getLong("dessertId").intValue());
                            od.setQuantity(        d.getLong("quantity").intValue());
                            od.setSize(            d.getLong("size").intValue());
                            orderedDessertDAO.insert(od);
                        }
                    });
                });
    }
}