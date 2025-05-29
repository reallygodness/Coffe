package com.example.cfeprjct.Activities.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cfeprjct.Adapters.CourierOrdersAdapter;
import com.example.cfeprjct.Entities.Address;
import com.example.cfeprjct.Entities.Order;
import com.example.cfeprjct.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

public class CourierOrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private CourierOrdersAdapter adapter;
    private List<Order> ordersList = new ArrayList<>();
    private Map<String, String> userNameMap = new HashMap<>();
    private Map<String, Address> addressMap = new HashMap<>();
    private String courierId;
    private com.google.firebase.firestore.ListenerRegistration ordersListener;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courier_orders, container, false);

        recyclerView = view.findViewById(R.id.recyclerCourierOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        courierId = getActivity().getIntent().getStringExtra("userId");

        adapter = new CourierOrdersAdapter(ordersList, userNameMap, addressMap, new CourierOrdersAdapter.OnTakeOrderClickListener() {
            @Override
            public void onTakeOrder(Order order) {
                takeOrder(order);
            }

            @Override
            public void onDelivered(Order order) {
                markOrderAsDelivered(order);
            }
        });
        adapter.setCourierId(courierId);
        recyclerView.setAdapter(adapter);

        loadOrders();

        new Handler().postDelayed(this::loadOrders, 30000);

        return view;
    }

    private void loadOrders() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders")
                .whereIn("statusId", List.of(1, 2))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ordersList.clear();
                    Set<String> userIds = new HashSet<>();
                    long now = System.currentTimeMillis();

                    Order activeOrder = null;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setFirestoreOrderId(doc.getId());

                        if (order.getStatusId() == 2 && courierId.equals(order.getCourierId())) {
                            activeOrder = order;
                            break;
                        }
                        if (order.getStatusId() == 1 && courierId.equals(order.getCourierId())) {
                            activeOrder = order;
                            break;
                        }
                    }

                    if (activeOrder != null) {
                        ordersList.add(activeOrder);
                        if (activeOrder.getUserId() != null) userIds.add(activeOrder.getUserId());
                    } else {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Order order = doc.toObject(Order.class);
                            order.setFirestoreOrderId(doc.getId());

                            if (order.getStatusId() == 1 && (order.getCourierId() == null || order.getCourierId().isEmpty())) {
                                ordersList.add(order);
                                if (order.getUserId() != null) userIds.add(order.getUserId());
                            }
                        }
                    }

                    loadUserNamesAndAddresses(new ArrayList<>(userIds));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки заказов: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    private void showError(String msg) {
        Toast.makeText(getContext(), "Ошибка загрузки заказов: " + msg, Toast.LENGTH_SHORT).show();
    }





    private void loadUserNamesAndAddresses(List<String> userIds) {
        if (userIds.isEmpty()) {
            adapter.setUserNameMap(userNameMap);
            adapter.setAddressMap(addressMap);
            adapter.notifyDataSetChanged();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereIn("userId", userIds)
                .get()
                .addOnSuccessListener(usersSnap -> {
                    userNameMap.clear();
                    for (QueryDocumentSnapshot doc : usersSnap) {
                        String userId = doc.getString("userId");
                        String firstName = doc.getString("firstName");
                        String lastName = doc.getString("lastName");
                        if (userId != null) {
                            String fio = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
                            userNameMap.put(userId, fio.trim());
                        }
                    }
                    db.collection("addresses")
                            .whereIn("userId", userIds)
                            .get()
                            .addOnSuccessListener(addrSnap -> {
                                addressMap.clear();
                                for (QueryDocumentSnapshot doc : addrSnap) {
                                    Address address = doc.toObject(Address.class);
                                    addressMap.put(address.getUserId(), address);
                                }
                                adapter.setUserNameMap(userNameMap);
                                adapter.setAddressMap(addressMap);
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                adapter.setUserNameMap(userNameMap);
                                adapter.setAddressMap(addressMap);
                                adapter.notifyDataSetChanged();
                            });
                })
                .addOnFailureListener(e -> {
                    adapter.setUserNameMap(userNameMap);
                    adapter.setAddressMap(addressMap);
                    adapter.notifyDataSetChanged();
                });
    }

    /** Курьер берёт заказ: courierId, courierTakeTime, статус не меняем! */
    private void takeOrder(Order order) {
        long now = System.currentTimeMillis();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Ставим courierId и courierTakeTime и меняем статус только в Firestore!
        db.collection("orders").document(order.getFirestoreOrderId())
                .update(
                        "courierId", courierId,
                        "courierTakeTime", now,
                        "statusId", 1 // пока не меняем на 2!
                ).addOnSuccessListener(aVoid -> {
                    // 2. Сохраняем в локальный Room (НЕОБЯЗАТЕЛЬНО, если не используешь Room для заказов у курьера)
                    // Если используешь — обнови локальный объект
                    new Thread(() -> {
                        order.setCourierId(courierId);
                        order.setCourierTakeTime(now);
                        order.setStatusId(1);
                        // orderDao.updateOrder(order); // если есть локальная таблица заказов
                    }).start();

                    Toast.makeText(getContext(), "Заказ взят!", Toast.LENGTH_SHORT).show();
                    loadOrders();
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка принятия заказа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /** Курьер отмечает "Доставлен" (статус = 3) */
    private void markOrderAsDelivered(Order order) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").document(order.getFirestoreOrderId())
                .update("statusId", 3, "deliveredTime", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Заказ отмечен как доставленный!", Toast.LENGTH_SHORT).show();
                    loadOrders();
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }



}
