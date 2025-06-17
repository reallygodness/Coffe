package com.example.cfeprjct.Activities.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cfeprjct.Adapters.CourierOrdersAdapter;
import com.example.cfeprjct.AppDatabase;
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

        subscribeToOrderUpdates();

        return view;
    }

    @Override
    public void onDestroyView() {
        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }
        super.onDestroyView();
    }

    private void subscribeToOrderUpdates() {
        if (ordersListener != null) {
            ordersListener.remove();
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        ordersListener = db.collection("orders")
                .whereIn("statusId", List.of(1, 2)) // Только актуальные
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        showError("Ошибка загрузки заказов: " + e.getMessage());
                        return;
                    }
                    ordersList.clear();
                    Set<String> userIds = new HashSet<>();
                    long now = System.currentTimeMillis();

                    // 1. Сначала ищем активный заказ курьера (statusId==2 и courierId==мой)
                    Order myActiveOrder = null;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setFirestoreOrderId(doc.getId());
                        if (order.getStatusId() == 2 && courierId.equals(order.getCourierId())) {
                            myActiveOrder = order;
                            break;
                        }
                    }

                    if (myActiveOrder != null) {
                        // Есть активный заказ - показываем только его!
                        ordersList.add(myActiveOrder);
                        if (myActiveOrder.getUserId() != null) userIds.add(myActiveOrder.getUserId());
                    } else {
                        // Нет активного - можно брать новый через 5 минут готовки
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Order order = doc.toObject(Order.class);
                            order.setFirestoreOrderId(doc.getId());
                            if (order.getStatusId() >= 3) continue;
                            if (order.getStatusId() == 1 && (order.getCourierId() == null || order.getCourierId().isEmpty())) {
                                long elapsed = now - order.getCreatedAt();
                                if (elapsed >= 5 * 60 * 1000) {
                                    ordersList.add(order);
                                    if (order.getUserId() != null) userIds.add(order.getUserId());
                                }
                            }
                            // Можно оставить блок с "Взятые этим курьером" если надо показывать таймер на 5 минут
                            else if (order.getStatusId() == 1 && courierId.equals(order.getCourierId())) {
                                long takeTime = order.getCourierTakeTime() != null ? order.getCourierTakeTime() : order.getCreatedAt();
                                ordersList.add(order);
                                if (order.getUserId() != null) userIds.add(order.getUserId());
                            }
                        }
                    }
                    loadUserNamesAndAddresses(new ArrayList<>(userIds));
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

    /** Курьер берёт заказ: обновляем courierId, statusId, deliveryStartTime */
    private void takeOrder(Order order) {
        long now = System.currentTimeMillis();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Обновляем заказ в Firestore (courierId, statusId=2, deliveryStartTime)
        db.collection("orders")
                .document(order.getFirestoreOrderId())
                .update("courierId", courierId,
                        "statusId", 2,
                        "deliveryStartTime", now)
                .addOnSuccessListener(aVoid -> {
                    // 2. Обновляем заказ локально в Room, если используешь локальное хранение
                    new Thread(() -> {
                        AppDatabase localDb = AppDatabase.getInstance(requireContext());
                        Order localOrder = localDb.orderDAO().getOrderById(order.getOrderId());
                        if (localOrder != null) {
                            localOrder.setCourierId(courierId);
                            localOrder.setStatusId(2);
                            localOrder.setDeliveryStartTime(now);
                            localDb.orderDAO().insertOrder(localOrder);
                        }
                    }).start();

                    Toast.makeText(getContext(), "Заказ взят! Везите заказ.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> showError("Ошибка при взятии заказа: " + e.getMessage()));
    }

    /** Курьер отмечает "Доставлен" (статус = 3) */
    private void markOrderAsDelivered(Order order) {
        long now = System.currentTimeMillis();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("orders")
                .document(order.getFirestoreOrderId())
                .update("statusId", 3, "deliveredTime", now)
                .addOnSuccessListener(aVoid -> {
                    // --- Сохраняем локально ---
                    new Thread(() -> {
                        AppDatabase localDb = AppDatabase.getInstance(requireContext());
                        Order localOrder = localDb.orderDAO().getOrderById(order.getOrderId());
                        if (localOrder != null) {
                            localOrder.setStatusId(3);
                            localOrder.setDeliveredTime(now);
                            localDb.orderDAO().insertOrder(localOrder); // обновит статус в локальной БД
                        }
                    }).start();

                    Toast.makeText(getContext(), "Заказ отмечен как доставленный!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> showError("Ошибка: " + e.getMessage()));
    }

}
