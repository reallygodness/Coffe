package com.example.cfeprjct.Adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cfeprjct.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCatalogAdapter extends RecyclerView.Adapter<AdminCatalogAdapter.ViewHolder> {


    private List<Map<String, Object>> products = new ArrayList<>();
    private final Context context;
    private String category;
    private final Runnable onProductUpdated;
    private final Runnable onProductDeleted;

    public AdminCatalogAdapter(Context context, String category, Runnable onProductUpdated, Runnable onProductDeleted) {
        this.context = context;
        this.category = category;
        this.onProductUpdated = onProductUpdated;
        this.onProductDeleted = onProductDeleted;
    }

    // Поменять категорию
    public void setCategory(String category) {
        this.category = category;
        products.clear();
        notifyDataSetChanged();
    }

    // Обновить список товаров
    public void setProducts(List<Map<String, Object>> products) {
        this.products = products != null ? products : new ArrayList<>();
        notifyDataSetChanged();
    }

    // Добавить временную карточку для нового товара
    public void addEmptyProductForEdit() {
        Map<String, Object> newItem = new HashMap<>();
        newItem.put("isEditMode", true);
        products.add(0, newItem);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public AdminCatalogAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_admin_catalog, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AdminCatalogAdapter.ViewHolder holder, int position) {
        Map<String, Object> item = products.get(position);

        boolean isEditMode = (item.containsKey("isEditMode") && (Boolean) item.get("isEditMode"));
        String name = (String) item.get("name");
        String description = (String) item.get("description");
        String imageUrl = (String) item.get("imageUrl");
        String id = item.get("id") != null ? String.valueOf(item.get("id")) : null;

        // === FIX: вес через поле size ===
        String weight = "";
        if ("dishes".equals(category) || "desserts".equals(category)) {
            if (item.get("size") != null) {
                weight = String.valueOf(item.get("size"));
            }
        }

        // --- VIEW MODE ---
        if (!isEditMode) {
            holder.editMode.setVisibility(View.GONE);
            holder.viewMode.setVisibility(View.VISIBLE);

            holder.tvName.setText(name != null ? name : "");
            holder.tvDesc.setText(description != null ? description : "");

            holder.tvPrice.setText(""); // сброс
            // === FIX: корректная загрузка цены из price_list ===
            if (id != null) {
                FirebaseFirestore.getInstance()
                        .collection("price_list").document(category)
                        .collection("items").document(id)
                        .get()
                        .addOnSuccessListener(priceDoc -> {
                            if (priceDoc.exists()) {
                                Number priceValue = priceDoc.getDouble("price");
                                if (priceValue == null) {
                                    Object priceObj = priceDoc.get("price");
                                    if (priceObj instanceof Long) priceValue = (Long) priceObj;
                                    else if (priceObj instanceof Integer) priceValue = (Integer) priceObj;
                                }
                                if (priceValue != null) {
                                    holder.tvPrice.setText(priceValue.intValue() + " ₽");
                                } else {
                                    holder.tvPrice.setText("Нет цены");
                                }
                            } else {
                                holder.tvPrice.setText("Нет цены");
                            }
                        })
                        .addOnFailureListener(e -> {
                            holder.tvPrice.setText("Ошибка");
                        });
            } else {
                holder.tvPrice.setText("");
            }

            // отображение веса только для блюд и десертов
            if ("dishes".equals(category) || "desserts".equals(category)) {
                holder.tvWeight.setVisibility(View.VISIBLE);
                holder.tvWeight.setText(!TextUtils.isEmpty(weight) ? "Вес: " + weight + " г" : "");
            } else {
                holder.tvWeight.setVisibility(View.GONE);
            }

            Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_placeholder).into(holder.ivImage);

            holder.btnEdit.setOnClickListener(v -> {
                item.put("isEditMode", true);
                notifyItemChanged(holder.getAdapterPosition());
            });
        }
        // --- EDIT MODE ---
        else {
            holder.viewMode.setVisibility(View.GONE);
            holder.editMode.setVisibility(View.VISIBLE);

            holder.etName.setText(name != null ? name : "");
            holder.etDesc.setText(description != null ? description : "");
            holder.etImageUrl.setText(imageUrl != null ? imageUrl : "");
            holder.etPrice.setText("");

            // === FIX: загрузка цены в режим редактирования ===
            if (id != null) {
                FirebaseFirestore.getInstance()
                        .collection("price_list").document(category)
                        .collection("items").document(id)
                        .get()
                        .addOnSuccessListener(priceDoc -> {
                            if (priceDoc.exists()) {
                                Number priceValue = priceDoc.getDouble("price");
                                if (priceValue == null) {
                                    Object priceObj = priceDoc.get("price");
                                    if (priceObj instanceof Long) priceValue = (Long) priceObj;
                                    else if (priceObj instanceof Integer) priceValue = (Integer) priceObj;
                                }
                                if (priceValue != null) {
                                    holder.etPrice.setText(String.valueOf(priceValue.intValue()));
                                }
                            }
                        });
            }
            // вес — только для блюд и десертов
            if ("dishes".equals(category) || "desserts".equals(category)) {
                holder.etWeight.setVisibility(View.VISIBLE);
                holder.etWeight.setText(weight != null ? weight : "");
            } else {
                holder.etWeight.setVisibility(View.GONE);
            }

            // Кнопка "Сохранить"
            holder.btnSave.setOnClickListener(v -> {
                int p = holder.getAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return;

                String newName = holder.etName.getText().toString().trim();
                String newDesc = holder.etDesc.getText().toString().trim();
                String newImg = holder.etImageUrl.getText().toString().trim();
                String newPrice = holder.etPrice.getText().toString().trim();
                String newWeight = holder.etWeight != null ? holder.etWeight.getText().toString().trim() : null;

                // Валидация
                if (newName.isEmpty() || newDesc.isEmpty() || newImg.isEmpty() || newPrice.isEmpty()
                        || (("dishes".equals(category) || "desserts".equals(category)) && (newWeight == null || newWeight.isEmpty()))
                ) {
                    Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    return;
                }

                // --- Формируем map для Firestore ---
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("name", newName);
                updateMap.put("description", newDesc);
                updateMap.put("imageUrl", newImg);
                if (("dishes".equals(category) || "desserts".equals(category)) && newWeight != null && !newWeight.isEmpty()) {
                    updateMap.put("size", Integer.parseInt(newWeight)); // поле size!
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                if (id != null) { // --- ОБНОВЛЕНИЕ ---
                    db.collection(category).document(id)
                            .set(updateMap)
                            .addOnSuccessListener(unused -> {
                                // === FIX: сохраняем price как новый документ с полями itemId, price, date, itemType ===
                                Map<String, Object> priceMap = new HashMap<>();
                                priceMap.put("itemId", Integer.valueOf(id));
                                priceMap.put("price", Double.parseDouble(newPrice));
                                priceMap.put("itemType", category.substring(0, category.length() - 1)); // "drink"/"dish"/"dessert"
                                priceMap.put("date", new java.util.Date());
                                db.collection("price_list").document(category)
                                        .collection("items").document(id)
                                        .set(priceMap);
                                Toast.makeText(context, "Изменения сохранены", Toast.LENGTH_SHORT).show();
                                item.putAll(updateMap);
                                item.put("isEditMode", false);
                                notifyItemChanged(p);
                                if (onProductUpdated != null) onProductUpdated.run();
                            });
                } else { // --- ДОБАВЛЕНИЕ ---
                    db.collection(category)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                int maxId = 0;
                                for (var doc : snapshot.getDocuments()) {
                                    try {
                                        int currId = Integer.parseInt(doc.getId());
                                        if (currId > maxId) maxId = currId;
                                    } catch (Exception ignored) {}
                                }
                                int newId = maxId + 1;

                                db.collection(category).document(String.valueOf(newId))
                                        .set(updateMap)
                                        .addOnSuccessListener(unused -> {
                                            Map<String, Object> priceMap = new HashMap<>();
                                            priceMap.put("itemId", newId);
                                            priceMap.put("price", Double.parseDouble(newPrice));
                                            priceMap.put("itemType", category.substring(0, category.length() - 1));
                                            priceMap.put("date", new java.util.Date());
                                            db.collection("price_list").document(category)
                                                    .collection("items").document(String.valueOf(newId))
                                                    .set(priceMap);
                                            Toast.makeText(context, "Товар добавлен", Toast.LENGTH_SHORT).show();
                                            updateMap.put("id", newId);
                                            products.set(p, updateMap);
                                            notifyItemChanged(p);
                                            if (onProductUpdated != null) onProductUpdated.run();
                                        });
                            });
                }
            });

            // Кнопка "Отмена"
            holder.btnCancel.setOnClickListener(v -> {
                int p = holder.getAdapterPosition();
                if (id == null) {
                    products.remove(p);
                    notifyItemRemoved(p);
                } else {
                    item.put("isEditMode", false);
                    notifyItemChanged(p);
                }
            });

            // Кнопка "Удалить"
            holder.btnDelete.setOnClickListener(v -> {
                int p = holder.getAdapterPosition();
                if (id != null) {
                    FirebaseFirestore.getInstance().collection(category).document(id)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                FirebaseFirestore.getInstance()
                                        .collection("price_list").document(category)
                                        .collection("items").document(id).delete();
                                Toast.makeText(context, "Товар удалён", Toast.LENGTH_SHORT).show();
                                products.remove(p);
                                notifyItemRemoved(p);
                                if (onProductDeleted != null) onProductDeleted.run();
                            });
                } else {
                    products.remove(p);
                    notifyItemRemoved(p);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // View mode
        View viewMode;
        ImageView ivImage;
        TextView tvName, tvDesc, tvPrice, tvWeight;
        ImageButton btnEdit;
        // Edit mode
        View editMode;
        EditText etName, etDesc, etImageUrl, etPrice, etWeight;
        ImageButton btnSave, btnCancel, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewMode = itemView.findViewById(R.id.viewModeBlock);
            ivImage  = itemView.findViewById(R.id.ivProductImage);
            tvName   = itemView.findViewById(R.id.tvProductName);
            tvDesc   = itemView.findViewById(R.id.tvProductDesc);
            tvPrice  = itemView.findViewById(R.id.tvProductPrice);
            tvWeight = itemView.findViewById(R.id.tvProductWeight);
            btnEdit  = itemView.findViewById(R.id.btnEdit);
            editMode   = itemView.findViewById(R.id.editModeBlock);
            etName     = itemView.findViewById(R.id.etProductName);
            etDesc     = itemView.findViewById(R.id.etProductDesc);
            etImageUrl = itemView.findViewById(R.id.etProductImageUrl);
            etPrice    = itemView.findViewById(R.id.etProductPrice);
            etWeight   = itemView.findViewById(R.id.etProductWeight);
            btnSave    = itemView.findViewById(R.id.btnSave);
            btnCancel  = itemView.findViewById(R.id.btnCancel);
            btnDelete  = itemView.findViewById(R.id.btnDelete);
        }
    }
}