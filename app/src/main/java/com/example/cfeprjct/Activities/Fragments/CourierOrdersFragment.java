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
                .whereIn("statusId", List.of(1, 2)) // только "В готовке" и "В доставке"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ordersList.clear();
                    Set<String> userIds = new HashSet<>();
                    long now = System.currentTimeMillis();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // ... [твой парсинг order, setOrderId, setFirestoreOrderId и т.д.]
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(Integer.parseInt(doc.getId()));
                        order.setFirestoreOrderId(doc.getId());

                        // Только активные:
                        // "В готовке" — свободные или взятые этим курьером
                        if (order.getStatusId() == 1 &&
                                ((order.getCourierId() == null || order.getCourierId().isEmpty()) ||
                                        courierId.equals(order.getCourierId()))) {
                            ordersList.add(order);
                        }
                        // "В доставке" только свои заказы
                        else if (order.getStatusId() == 2 && courierId.equals(order.getCourierId())) {
                            ordersList.add(order);
                        }
                        // Всё остальное не добавляем!
                        if (order.getUserId() != null) userIds.add(order.getUserId());
                    }
                    loadUserNamesAndAddresses(new ArrayList<>(userIds));
                });

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
        long takeTime = System.currentTimeMillis();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").document(order.getFirestoreOrderId())
                .update("courierId", courierId, "courierTakeTime", takeTime)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Заказ принят! Ожидайте приготовления...", Toast.LENGTH_SHORT).show();
                    loadOrders();
                })
                .addOnFailureListener(e -> {
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
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



}
