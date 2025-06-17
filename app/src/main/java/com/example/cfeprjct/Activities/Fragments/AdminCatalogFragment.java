package com.example.cfeprjct.Activities.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cfeprjct.Adapters.AdminCatalogAdapter;
import com.example.cfeprjct.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AdminCatalogFragment extends Fragment {
        private AdminCatalogAdapter adapter;
        private String selectedCategory = "drinks"; // По умолчанию

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_catalog, container, false);

        RecyclerView rv = view.findViewById(R.id.rvAdminCatalog);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminCatalogAdapter(
                getContext(),
                selectedCategory,
                this::loadCatalog,
                () -> Toast.makeText(getContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
        );
        rv.setAdapter(adapter);

        Button btnAddProduct = view.findViewById(R.id.btnAddProduct);
        btnAddProduct.setOnClickListener(v -> {
            adapter.addEmptyProductForEdit();
            rv.scrollToPosition(0);
        });

        // Кнопки категорий
        view.findViewById(R.id.btnDrinks).setOnClickListener(v -> {
            selectedCategory = "drinks";
            adapter = new AdminCatalogAdapter(
                    getContext(),
                    selectedCategory,
                    this::loadCatalog,
                    () -> Toast.makeText(getContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
            );
            rv.setAdapter(adapter);
            loadCatalog();
        });
        view.findViewById(R.id.btnDishes).setOnClickListener(v -> {
            selectedCategory = "dishes";
            adapter = new AdminCatalogAdapter(
                    getContext(),
                    selectedCategory,
                    this::loadCatalog,
                    () -> Toast.makeText(getContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
            );
            rv.setAdapter(adapter);
            loadCatalog();
        });
        view.findViewById(R.id.btnDesserts).setOnClickListener(v -> {
            selectedCategory = "desserts";
            adapter = new AdminCatalogAdapter(
                    getContext(),
                    selectedCategory,
                    this::loadCatalog,
                    () -> Toast.makeText(getContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
            );
            rv.setAdapter(adapter);
            loadCatalog();
        });

        loadCatalog();


        return view;
    }

        private void loadCatalog() {
            // Получить товары из Firestore
            FirebaseFirestore.getInstance()
                    .collection(selectedCategory)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Map<String, Object>> products = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Map<String, Object> data = doc.getData();
                            data.put("id", doc.getId());
                            products.add(data);
                        }
                        adapter.setProducts(products);
                    });
        }
    }

