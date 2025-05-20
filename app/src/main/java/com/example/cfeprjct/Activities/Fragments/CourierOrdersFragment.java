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
    private Map<String, String> userNameMap = new HashMap<>();      // userId → ФИО
    private Map<String, Address> addressMap = new HashMap<>();      // userId → Address

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courier_orders, container, false);

        recyclerView = view.findViewById(R.id.recyclerCourierOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CourierOrdersAdapter(ordersList, userNameMap, addressMap, order -> {
            takeOrder(order);
        });
        recyclerView.setAdapter(adapter);

        loadOrders();

        return view;
    }

    private void loadOrders() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders")
                .whereEqualTo("statusId", 2) // Статус "Готов к доставке"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ordersList.clear();
                    Set<String> userIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setFirestoreOrderId(doc.getId());
                        ordersList.add(order);
                        if (order.getUserId() != null) userIds.add(order.getUserId());
                    }
                    loadUserNamesAndAddresses(new ArrayList<>(userIds));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки заказов: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Загружаем имена и адреса всех клиентов для заказов
    private void loadUserNamesAndAddresses(List<String> userIds) {
        if (userIds.isEmpty()) {
            adapter.setUserNameMap(userNameMap);
            adapter.setAddressMap(addressMap);
            adapter.notifyDataSetChanged();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Загружаем имена
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
                    // Загружаем адреса после имен
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

    private void takeOrder(Order order) {
        String courierId = getActivity().getIntent().getStringExtra("userId");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("orders").document(order.getFirestoreOrderId())
                .update("courierId", courierId, "statusId", 3)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Заказ принят!", Toast.LENGTH_SHORT).show();
                    loadOrders(); // обновить список
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка принятия заказа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


}
