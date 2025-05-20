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

import com.example.cfeprjct.Entities.Order;
import com.example.cfeprjct.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class CourierOrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private CourierOrdersAdapter adapter;
    private List<Order> ordersList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courier_orders, container, false);

        recyclerView = view.findViewById(R.id.recyclerCourierOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CourierOrdersAdapter(ordersList, order -> {
            // обработка нажатия на "Взять заказ" (например, обновить статус и courierId)
            takeOrder(order);
        });
        recyclerView.setAdapter(adapter);

        loadOrders();

        return view;
    }


    private void loadOrders() {
        // Загрузка заказов со статусом "Готов к доставке" (например, statusId = 2)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders")
                .whereEqualTo("statusId", 2)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ordersList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        ordersList.add(order);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки заказов: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void takeOrder(Order order) {
        // Пример: назначить courierId и поменять статус заказа на "В доставке" (statusId = 3)
        // Здесь должен быть userId текущего курьера
        String courierId = getActivity().getIntent().getStringExtra("userId");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("orders").document(order.getOrderId())
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
