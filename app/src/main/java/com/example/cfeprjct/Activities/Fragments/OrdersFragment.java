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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
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

    // Потокобезопасный список слушателей!
    private final List<ListenerRegistration> orderListeners = Collections.synchronizedList(new ArrayList<>());

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
                        .set(om, com.google.firebase.firestore.SetOptions.merge());
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
                    clearOrderListeners();
                    subscribeToOrderStatusUpdates();
                });
            });
        });

        return view;
    }
    private void clearOrderListeners() {
        synchronized (orderListeners) {
            for (ListenerRegistration l : orderListeners) {
                l.remove();
            }
            orderListeners.clear();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearOrderListeners();
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
                int orderId = doc.getLong("orderId").intValue();
                // Проверяем, есть ли уже заказ локально
                Order existing = orderDAO.getOrderById(orderId);
                if (existing == null) {
                    Order o = new Order();
                    o.setOrderId(orderId);
                    o.setUserId(doc.getString("userId"));
                    o.setCreatedAt(doc.getLong("createdAt"));
                    o.setTotalPrice(doc.getDouble("totalPrice").floatValue());
                    o.setStatusId(doc.getLong("statusId").intValue());
                    orderDAO.insertOrder(o);

                    String orderDocId = doc.getId();
                    // импорт позиций — только если заказа не было!
                    importOrderPositions(orderDocId);
                } else {
                    // если заказ уже есть, просто обновляем статус
                    Integer statusId = doc.getLong("statusId") != null ? doc.getLong("statusId").intValue() : null;
                    if (statusId != null && existing.getStatusId() != statusId) {
                        existing.setStatusId(statusId);
                        orderDAO.insertOrder(existing);
                    }
                }
            }
        });
    }


    private void importOrderPositions(String orderDocId) {
        int orderId = Integer.parseInt(orderDocId);
        // УДАЛЯЕМ только если действительно импортируем заказ (т.е. заказа ещё не было), иначе НЕ удаляем!
        // Если ты вызываешь этот метод ТОЛЬКО при первом импорте заказа, оставь как есть.
        // Если можешь вызываться при апдейте, тогда удали строки deleteByOrderId!
        orderedDrinkDAO.deleteByOrderId(orderId);
        orderedDishDAO.deleteByOrderId(orderId);
        orderedDessertDAO.deleteByOrderId(orderId);

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
                            od.setOrderId(d.getLong("orderId").intValue());
                            od.setDrinkId(d.getLong("drinkId").intValue());
                            od.setQuantity(d.getLong("quantity").intValue());
                            od.setSize(d.getLong("size").intValue());

                            // Проверка на дубликаты
                            if (orderedDrinkDAO.getByOrderId(od.getOrderedDrinkId()) == null) {
                                orderedDrinkDAO.insert(od);
                            }
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
                            od.setOrderId(d.getLong("orderId").intValue());
                            od.setDishId(d.getLong("dishId").intValue());
                            od.setQuantity(d.getLong("quantity").intValue());
                            od.setSize(d.getLong("size").intValue());

                            if (orderedDishDAO.getByOrderId(od.getOrderedDishId()) == null) {
                                orderedDishDAO.insert(od);
                            }
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
                            od.setOrderId(d.getLong("orderId").intValue());
                            od.setDessertId(d.getLong("dessertId").intValue());
                            od.setQuantity(d.getLong("quantity").intValue());
                            od.setSize(d.getLong("size").intValue());
                            if (orderedDessertDAO.getByOrderId(od.getOrderedDessertId()) == null) {
                                orderedDessertDAO.insert(od);
                            }
                        }
                    });
                });
    }


    private void subscribeToOrderStatusUpdates() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        new Thread(() -> {
            List<Order> orders = orderDAO.getAllSync();
            synchronized (orderListeners) {
                for (Order order : orders) {
                    ListenerRegistration listener = firestore.collection("orders")
                            .document(String.valueOf(order.getOrderId()))
                            .addSnapshotListener((doc, error) -> {
                                if (error != null || doc == null || !doc.exists()) return;
                                Integer statusId = doc.getLong("statusId") != null ? doc.getLong("statusId").intValue() : null;
                                Long deliveredTime = doc.getLong("deliveredTime");
                                Long deliveryStartTime = doc.getLong("deliveryStartTime");

                                if (statusId != null) {
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        Order localOrder = orderDAO.getOrderById(order.getOrderId());
                                        if (localOrder != null) {
                                            boolean changed = false;
                                            if (localOrder.getStatusId() != statusId) {
                                                localOrder.setStatusId(statusId);
                                                changed = true;
                                            }
                                            if (deliveredTime != null && (localOrder.getDeliveredTime() == null || !deliveredTime.equals(localOrder.getDeliveredTime()))) {
                                                localOrder.setDeliveredTime(deliveredTime);
                                                changed = true;
                                            }
                                            if (deliveryStartTime != null && (localOrder.getDeliveryStartTime() == null || !deliveryStartTime.equals(localOrder.getDeliveryStartTime()))) {
                                                localOrder.setDeliveryStartTime(deliveryStartTime);
                                                changed = true;
                                            }
                                            if (changed) orderDAO.insertOrder(localOrder); // Только upsert, не трогать позиции!
                                        }
                                    });
                                }
                            });
                    orderListeners.add(listener);
                }
            }
        }).start();
    }


}
