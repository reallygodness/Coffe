package com.example.cfeprjct.Sync;

import android.content.Context;
import android.util.Log;

import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.Entities.Dessert;
import com.example.cfeprjct.Entities.Dish;
import com.example.cfeprjct.Entities.Drink;
import com.example.cfeprjct.Entities.PriceList;
import com.example.cfeprjct.Entities.Volume;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CatalogSync {
    private final AppDatabase db;
    private final FirebaseFirestore firestore;

    public interface Callback {
        void onComplete();
    }

    public CatalogSync(Context ctx) {
        db = AppDatabase.getInstance(ctx.getApplicationContext());
        firestore = FirebaseFirestore.getInstance();
    }

    /** 1) Синхронизируем напитки, а затем цены */
    public void syncDrinks(Callback cb) {
        firestore.collection("drinks")
                .get()
                .addOnSuccessListener(qs -> {
                    List<Drink> drinks = new ArrayList<>();
                    for (DocumentSnapshot doc : qs) {
                        Drink d = new Drink();
                        try {
                            d.setDrinkId(Integer.parseInt(doc.getId()));
                        } catch (NumberFormatException e) {
                            Log.w("CatalogSync", "Неверный формат id напитка: " + doc.getId());
                            continue;
                        }
                        d.setName(doc.getString("name"));
                        d.setDescription(doc.getString("description"));
                        d.setImageUrl(doc.getString("imageUrl"));
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> vols = (List<Map<String, Object>>) doc.get("volumes");
                        if (vols != null && !vols.isEmpty()) {
                            Object vid = vols.get(0).get("volumeId");
                            if (vid instanceof Number) {
                                d.setVolumeId(((Number) vid).intValue());
                            }
                        }
                        drinks.add(d);
                    }
                    new Thread(() -> {
                        db.drinkDAO().insertAll(drinks);
                        // после напитков синхронизируем цены
                        syncPrices(cb);
                    }).start();
                })
                .addOnFailureListener(e -> {
                    Log.e("CatalogSync", "Ошибка загрузки drinks", e);
                    // даже при ошибке — пытаемся синхронизировать цены
                    syncPrices(cb);
                });
    }

    /** 2) Синхронизируем блюда */
    public void syncDishes(Callback cb) {
        firestore.collection("dishes")
                .get()
                .addOnSuccessListener(qs -> {
                    List<Dish> list = new ArrayList<>();
                    for (DocumentSnapshot doc : qs) {
                        Dish d = new Dish();
                        d.setDishId(Integer.parseInt(doc.getId()));
                        d.setName(doc.getString("name"));
                        d.setDescription(doc.getString("description"));
                        d.setImageUrl(doc.getString("imageUrl"));

                        Long sz = doc.getLong("size"); // ← читаем size
                        d.setSize(sz != null ? sz.intValue() : 0);
                        list.add(d);
                    }
                    new Thread(() -> {
                        db.dishDAO().insertAll(list);
                        cb.onComplete();
                    }).start();
                })
                .addOnFailureListener(e -> {
                    Log.e("CatalogSync", "Ошибка загрузки dishes", e);
                    cb.onComplete();
                });
    }

    /** 3) Синхронизируем десерты */
    public void syncDesserts(Callback cb) {
        firestore.collection("desserts")
                .get()
                .addOnSuccessListener(qs -> {
                    List<Dessert> list = new ArrayList<>();
                    for (DocumentSnapshot doc : qs) {
                        Dessert d = new Dessert();
                        d.setDessertId(Integer.parseInt(doc.getId()));
                        d.setName(doc.getString("name"));
                        d.setDescription(doc.getString("description"));
                        d.setImageUrl(doc.getString("imageUrl"));
                        Long sz = doc.getLong("size");
                        d.setSize(sz != null ? sz.intValue() : 0);
                        list.add(d);
                    }
                    new Thread(() -> {
                        db.dessertDAO().insertAll(list);
                        cb.onComplete();
                    }).start();
                })
                .addOnFailureListener(e -> {
                    Log.e("CatalogSync", "Ошибка загрузки desserts", e);
                    cb.onComplete();
                });
    }

    public void syncPrices(Callback cb) {
        // 1) Три отдельных запроса: для drinks, dishes и desserts
        Task<QuerySnapshot> drinksTask = firestore
                .collection("price_list")
                .document("drinks")
                .collection("items")
                .get();

        Task<QuerySnapshot> dishesTask = firestore
                .collection("price_list")
                .document("dishes")
                .collection("items")
                .get();

        Task<QuerySnapshot> dessertsTask = firestore
                .collection("price_list")
                .document("desserts")
                .collection("items")
                .get();

        // 2) Когда все три запроса успешно завершатся — обрабатываем их в порядке t1, t2, t3
        Tasks.whenAllSuccess(drinksTask, dishesTask, dessertsTask)
                .addOnSuccessListener(results -> {
                    List<PriceList> all = new ArrayList<>();
                    for (int i = 0; i < results.size(); i++) {
                        QuerySnapshot snap = (QuerySnapshot) results.get(i);
                        // Определяем категорию по порядковому номеру таска
                        String type = (i == 0 ? "drinks" : i == 1 ? "dishes" : "desserts");

                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Long itemIdLong = doc.getLong("itemId");
                            Double priceD   = doc.getDouble("price");
                            Date   dt       = doc.getDate("date");
                            if (itemIdLong == null || priceD == null) continue;

                            PriceList p = new PriceList();
                            // Записываем id в нужное поле
                            switch (type) {
                                case "drinks":  p.setDrinkId(itemIdLong.intValue());   break;
                                case "dishes":  p.setDishId(itemIdLong.intValue());    break;
                                default:        p.setDessertId(itemIdLong.intValue()); break;
                            }
                            p.setPrice(priceD.floatValue());
                            p.setDate(dt != null ? dt.getTime() : System.currentTimeMillis());
                            all.add(p);
                        }
                    }

                    // 3) Сохраняем всё в Room и отрабатываем коллбэк
                    new Thread(() -> {
                        db.priceListDAO().insertAll(all);
                        Log.d("CatalogSync", "Saved prices: " + all.size());
                        cb.onComplete();
                    }).start();
                })
                .addOnFailureListener(e -> {
                    Log.e("CatalogSync", "Failed to load price_list", e);
                    cb.onComplete();
                });
    }


    /** Шаг 0: синхронизируем объёмы из Firestore → Room.volumes */
    public void syncVolumes(Callback cb) {
        firestore.collection("volumes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Volume> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        // Для каждого поля в документе (S, M, L)
                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            String sizeKey = entry.getKey();        // "S", "M" или "L"
                            Object rawValue = entry.getValue();
                            if (!(rawValue instanceof Number)) continue;
                            int ml = ((Number) rawValue).intValue();

                            Volume v = new Volume();
                            v.setSize(sizeKey);
                            v.setMl(ml);
                            // Не вызываем v.setVolumeId(), чтобы Room авто‑присвоил ID
                            list.add(v);
                        }
                    }
                    // Пишем результат в Room в фоне
                    new Thread(() -> {
                        db.volumeDAO().insertAll(list);
                        Log.d("CatalogSync", "Room: записано объёмов = " + list.size());
                        cb.onComplete();
                    }).start();
                })
                .addOnFailureListener(e -> {
                    Log.e("CatalogSync", "Ошибка при загрузке volumes", e);
                    cb.onComplete();
                });
    }

}
