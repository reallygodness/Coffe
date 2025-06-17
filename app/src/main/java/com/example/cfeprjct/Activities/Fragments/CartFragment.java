package com.example.cfeprjct.Activities.Fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cfeprjct.Activities.MainActivity;
import com.example.cfeprjct.Adapters.CartAdapter;
import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.AuthUtils;
import com.example.cfeprjct.DAOS.AddressDAO;
import com.example.cfeprjct.DAOS.OrderDAO;
import com.example.cfeprjct.DAOS.OrderStatusDAO;
import com.example.cfeprjct.DAOS.OrderedDessertDAO;
import com.example.cfeprjct.DAOS.OrderedDishDAO;
import com.example.cfeprjct.DAOS.OrderedDrinkDAO;
import com.example.cfeprjct.Entities.Address;
import com.example.cfeprjct.Entities.CartItem;
import com.example.cfeprjct.Entities.Order;
import com.example.cfeprjct.Entities.OrderStatus;
import com.example.cfeprjct.Entities.OrderedDessert;
import com.example.cfeprjct.Entities.OrderedDish;
import com.example.cfeprjct.Entities.OrderedDrink;
import com.example.cfeprjct.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class CartFragment extends Fragment {
    private static final int REQ_LOC = 1001;
    private static final String PREFS = "app_prefs";
    private static final String KEY_LAST_USER = "currentUserId";

    private AppDatabase db;
    private AddressDAO addressDAO;
    private OrderDAO orderDAO;
    private OrderedDrinkDAO orderedDrinkDAO;
    private OrderedDishDAO orderedDishDAO;
    private OrderedDessertDAO orderedDessertDAO;
    private OrderStatusDAO orderStatusDAO;
    private FirebaseFirestore firestore;
    private FusedLocationProviderClient fusedLocationClient;

    private CartAdapter cartAdapter;
    private RecyclerView rvCart;
    private TextView tvTotal, tvAddress, tvEmptyCart, tvAvailableBonuses;
    private ImageView btnEditAddress;
    private MaterialButton btnCheckout;

    private EditText etUseBonuses;

    private final Set<Integer> previousCartItemIds = new HashSet<>();
    private boolean hasAddress = false;
    private boolean cartLoadedFromCloud = false;

    private long userBonuses = 0;

    private int cartSum = 0;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        // 1) Инициализация БД, DAO и сервисов
        db = AppDatabase.getInstance(requireContext());
        addressDAO = db.addressDAO();
        orderDAO = db.orderDAO();
        orderedDrinkDAO = db.orderedDrinkDAO();
        orderedDishDAO = db.orderedDishDAO();
        orderedDessertDAO = db.orderedDessertDAO();
        orderStatusDAO = db.orderStatusDAO();
        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());


        String userId = AuthUtils.getLoggedInUserId(requireContext());

        // 2) Очищаем локальную корзину при смене пользователя
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String lastUser = prefs.getString(KEY_LAST_USER, null);
        if (userId != null && !userId.equals(lastUser)) {
            Executors.newSingleThreadExecutor().execute(db.cartItemDao()::clearAll);
            prefs.edit().putString(KEY_LAST_USER, userId).apply();
        }

        // 3) View binding
        rvCart = view.findViewById(R.id.rvCart);
        tvTotal = view.findViewById(R.id.tvCartTotal);
        tvAddress = view.findViewById(R.id.tvCartAddress);
        btnEditAddress = view.findViewById(R.id.btnEditAddress);
        btnCheckout = view.findViewById(R.id.btnCheckout);
        tvEmptyCart = view.findViewById(R.id.tvEmptyCart);
        tvAvailableBonuses = view.findViewById(R.id.tvAvailableBonuses);
        etUseBonuses = view.findViewById(R.id.etUseBonuses);

        // 4) RecyclerView
        rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartAdapter = new CartAdapter(db.cartItemDao());
        rvCart.setAdapter(cartAdapter);

        // 5) Загрузка адреса
        loadAddress(userId);

        // 6) Первичная подгрузка корзины из Firestore (только один раз)
        if (userId != null && !cartLoadedFromCloud) {
            loadCartFromCloud(userId);
        }

        // 7) Обработчики кликов по адресу
        View.OnClickListener addrClick = v -> {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQ_LOC);
            } else {
                requestLastLocation();
            }
        };
        tvAddress.setOnClickListener(addrClick);
        btnEditAddress.setOnClickListener(v -> showAddressInputDialog());

        // 8) Подписываемся на корзину и синхронизируем
        observeCart(userId);

        // 9) Оформление заказа
        btnCheckout.setOnClickListener(v -> {
            if (!hasAddress) {
                Toast.makeText(requireContext(),
                        "Введите адрес для заказа", Toast.LENGTH_SHORT).show();
                return;
            }
            syncStatusesThenCheckout();
        });

        loadUserBonuses();

        etUseBonuses.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString().trim();
                if (str.isEmpty()) {
                    updateTotalWithBonuses();
                    return;
                }
                try {
                    long inputBonuses = Long.parseLong(str);
                    // Максимум: не больше чем у пользователя и не больше 50% стоимости
                    int total = cartSum + 100;
                    float maxDiscountRub = total * 0.5f;
                    long maxBonusesToUse = Math.min(userBonuses, (long) (maxDiscountRub * 10));
                    if (inputBonuses > maxBonusesToUse) {
                        // Обрезаем до максимального значения
                        etUseBonuses.removeTextChangedListener(this);
                        etUseBonuses.setText(String.valueOf(maxBonusesToUse));
                        etUseBonuses.setSelection(etUseBonuses.getText().length());
                        etUseBonuses.addTextChangedListener(this);
                    }
                } catch (NumberFormatException e) {
                    // Если что-то не так — просто очищаем поле
                    etUseBonuses.removeTextChangedListener(this);
                    etUseBonuses.setText("");
                    etUseBonuses.addTextChangedListener(this);
                }
                updateTotalWithBonuses();
            }
        });




        return view;
    }



    // ——— ADDRESS LOGIC ———
    private void loadAddress(@Nullable String userId) {
        if (userId == null) {
            tvAddress.setText("Введите адрес");
            hasAddress = false;
            updateCheckoutState(0);
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            Address local = addressDAO.getAddressByUserId(userId);
            if (local != null) {
                String fmt = local.getCity() + ", " + local.getStreet() + " " + local.getHouse()
                        + (local.getApartment().isEmpty() ? "" : ", кв. " + local.getApartment());
                requireActivity().runOnUiThread(() -> {
                    tvAddress.setText(fmt);
                    hasAddress = true;
                    updateCheckoutState(0);
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    tvAddress.setText("Введите адрес");
                    hasAddress = false;
                    updateCheckoutState(0);
                });
            }
        });
    }

    private void saveAndSyncAddress(String city,
                                    String street,
                                    String house,
                                    String apartment) {
        String userId = AuthUtils.getLoggedInUserId(requireContext());
        if (userId == null) return;
        new Thread(() -> {
            Address ex = addressDAO.getAddressByUserId(userId);
            int addrId;
            if (ex != null) {
                ex.setCity(city);
                ex.setStreet(street);
                ex.setHouse(house);
                ex.setApartment(apartment);
                addressDAO.updateAddress(ex);
                addrId = ex.getAddressId();
            } else {
                Address a = new Address();
                a.setUserId(userId);
                a.setCity(city);
                a.setStreet(street);
                a.setHouse(house);
                a.setApartment(apartment);
                addrId = (int) addressDAO.insertAddress(a);
            }
            Map<String, Object> m = new HashMap<>();
            m.put("addressId", addrId);
            m.put("userId",    userId);
            m.put("city",      city);
            m.put("street",    street);
            m.put("house",     house);
            m.put("apartment", apartment);
            firestore.collection("addresses")
                    .document(String.valueOf(addrId))
                    .set(m);
            firestore.collection("users")
                    .document(userId)
                    .update("addressId", addrId);
            String fmt = city + ", " + street + " " + house
                    + (apartment.isEmpty() ? "" : ", кв. " + apartment);
            requireActivity().runOnUiThread(() -> {
                tvAddress.setText(fmt);
                hasAddress = true;
                updateCheckoutState(0);
            });
        }).start();
    }

    @SuppressLint("MissingPermission")
    private void requestLastLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) reverseGeocode(loc.getLatitude(), loc.getLongitude());
                    else Toast.makeText(requireContext(),
                            "Не удалось получить локацию", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Ошибка при получении геолокации", Toast.LENGTH_SHORT).show());
    }

    private void reverseGeocode(double lat, double lng) {
        new Thread(() -> {
            try {
                Geocoder gc = new Geocoder(requireContext());
                List<android.location.Address> list = gc.getFromLocation(lat, lng, 1);
                if (!list.isEmpty()) {
                    android.location.Address src = list.get(0);
                    saveAndSyncAddress(src.getLocality(),
                            src.getThoroughfare(),
                            src.getFeatureName(),
                            "");
                }
            } catch (IOException ignored) {}
        }).start();
    }

    // ——— CART SYNC LOGIC ———
    private void loadCartFromCloud(String userId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<CartItem> local = db.cartItemDao().getAllSync();
            if (local.isEmpty()) {
                firestore.collection("carts").document(userId)
                        .collection("items").get()
                        .addOnSuccessListener(query -> {
                            List<CartItem> toInsert = new ArrayList<>();
                            for (DocumentSnapshot d : query.getDocuments()) {
                                CartItem ci = d.toObject(CartItem.class);
                                if (ci != null) toInsert.add(ci);
                            }
                            Executors.newSingleThreadExecutor().execute(() -> {
                                for (CartItem ci : toInsert) db.cartItemDao().insert(ci);
                                cartLoadedFromCloud = true;
                            });
                        });
            } else {
                cartLoadedFromCloud = true;
            }
        });
    }

    private void observeCart(String userId) {
        LiveData<List<CartItem>> live = db.cartItemDao().getAll();
        live.observe(getViewLifecycleOwner(), items -> {
            cartAdapter.submitList(items);

            cartSum = 0;
            for (CartItem ci : items) cartSum += ci.getQuantity() * ci.getUnitPrice();
            if (items.isEmpty()) {
                tvEmptyCart.setVisibility(View.VISIBLE);
                tvTotal.setVisibility(View.GONE);
                btnCheckout.setVisibility(View.GONE);

                // ————— При пустой корзине удаляем все документы в Firestore —————
                firestore.collection("carts")
                        .document(userId)
                        .collection("items")
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                doc.getReference().delete();
                            }
                            previousCartItemIds.clear();
                        });
            } else {
                tvEmptyCart.setVisibility(View.GONE);
                tvTotal.setVisibility(View.VISIBLE);
                btnCheckout.setVisibility(View.VISIBLE);
                updateTotalWithBonuses(); // <-- Всегда пересчитывай итог с учетом бонусов!
                updateCheckoutState(cartSum);

                Executors.newSingleThreadExecutor().execute(() ->
                        syncCartItemsToFirestore(userId, items)
                );
            }
        });
    }

    private void updateTotalWithBonuses() {
        // cartSum — сумма всех товаров (без доставки), доставка всегда +100
        int total = cartSum + 100;
        String input = etUseBonuses.getText().toString().trim();
        long bonusesToUse = 0;
        try {
            bonusesToUse = input.isEmpty() ? 0 : Long.parseLong(input);
        } catch (NumberFormatException ignored) {}

        // Ограничения: максимум 50% от суммы заказа, не больше userBonuses
        float maxDiscountRub = total * 0.5f;
        long maxBonusesToUse = Math.min(userBonuses, (long) (maxDiscountRub * 10));
        if (bonusesToUse > maxBonusesToUse) bonusesToUse = maxBonusesToUse;

        float discountRub = bonusesToUse / 10f;
        float totalAfterDiscount = total - discountRub;
        if (totalAfterDiscount < 0) totalAfterDiscount = 0;

        tvTotal.setText(String.format(Locale.getDefault(),
                "Итог: %d ₽ (доставка 100 ₽)", (int) totalAfterDiscount));
    }



    private void syncCartItemsToFirestore(String userId, List<CartItem> items) {
        CollectionReference col = firestore.collection("carts")
                .document(userId)
                .collection("items");
        Set<Integer> newIds = new HashSet<>();
        for (CartItem ci : items) {
            newIds.add(ci.getId());
            if (!previousCartItemIds.contains(ci.getId())) {
                Map<String,Object> m = new HashMap<>();
                m.put("id",            ci.getId());
                m.put("productType",   ci.getProductType());
                m.put("productId",     ci.getProductId());
                m.put("title",         ci.getTitle());
                m.put("imageUrl",      ci.getImageUrl());
                m.put("size",          ci.getSize());
                m.put("unitPrice",     ci.getUnitPrice());
                m.put("quantity",      ci.getQuantity());
                col.document(String.valueOf(ci.getId())).set(m);
            }
        }
        for (Integer oldId : previousCartItemIds) {
            if (!newIds.contains(oldId)) {
                col.document(String.valueOf(oldId)).delete();
            }
        }
        previousCartItemIds.clear();
        previousCartItemIds.addAll(newIds);
    }

    // ——— ORDER CREATION LOGIC ———
    private void syncStatusesThenCheckout() {
        new Thread(() -> {
            List<OrderStatus> stats = orderStatusDAO.getAllStatuses();
            if (stats.isEmpty()) {
                firestore.collection("order_statuses").get()
                        .addOnSuccessListener(q -> Executors.newSingleThreadExecutor().execute(() -> {
                            for (DocumentSnapshot d : q.getDocuments()) {
                                OrderStatus st = new OrderStatus();
                                st.setStatusId(d.getLong("statusId").intValue());
                                st.setStatusName(d.getString("statusName"));
                                orderStatusDAO.insert(st);
                            }
                            performCheckout();
                        }));
            } else {
                performCheckout();
            }
        }).start();
    }

    private void performCheckout() {
        List<CartItem> items = db.cartItemDao().getAllSync();
        if (items.isEmpty() || !hasAddress) return;

        long now = System.currentTimeMillis();
        OrderStatus cook = orderStatusDAO.getByName("В готовке");
        int cookId = cook != null ? cook.getStatusId() : 1;
        float total = 100f;
        for (CartItem ci : items) total += ci.getQuantity() * ci.getUnitPrice();

        String userId = AuthUtils.getLoggedInUserId(requireContext());
        int lastOrderNum = orderDAO.getMaxUserOrderNumber(userId);
        if (lastOrderNum < 0) lastOrderNum = 0;
        int nextOrderNum = lastOrderNum + 1;

        // === 1. Считываем доступные бонусы и сколько хочет списать ===
        long bonusesToUse = 0;
        try {
            String input = etUseBonuses.getText().toString().trim();
            bonusesToUse = input.isEmpty() ? 0 : Long.parseLong(input);
        } catch (NumberFormatException ignored) {}
        if (bonusesToUse > userBonuses) bonusesToUse = userBonuses;

        float maxDiscountRub = total * 0.5f; // максимум 50% суммы заказа
        long maxBonusesToUse = Math.min(userBonuses, (long) (maxDiscountRub * 10));
        if (bonusesToUse > maxBonusesToUse) bonusesToUse = maxBonusesToUse;

        float discountRub = bonusesToUse / 10f; // 10 бонусов = 1 рубль
        float totalAfterDiscount = total - discountRub;
        if (totalAfterDiscount < 0) totalAfterDiscount = 0;

        // === 2. Создаём Order (Room) ===
        Order order = new Order();
        order.setUserId(userId);
        order.setCreatedAt(now);
        order.setStatusId(cookId);
        order.setTotalPrice(totalAfterDiscount); // уже с учетом скидки
        order.setUserOrderNumber(nextOrderNum);
        int orderId = (int) orderDAO.insertOrder(order); // локальный Room id (и Firestore id будет такой же!)

        // Вставляем позиции в зависимости от типа
        for (CartItem ci : items) {
            switch (ci.getProductType()) {
                case "drink":
                    OrderedDrink drink = new OrderedDrink();
                    drink.setOrderId(orderId);
                    drink.setDrinkId(ci.getProductId());
                    drink.setQuantity(ci.getQuantity());
                    drink.setSize(ci.getSize());
                    orderedDrinkDAO.insert(drink);
                    break;
                case "dish":
                    OrderedDish dish = new OrderedDish();
                    dish.setOrderId(orderId);
                    dish.setDishId(ci.getProductId());
                    dish.setQuantity(ci.getQuantity());
                    dish.setSize(ci.getSize());
                    orderedDishDAO.insert(dish);
                    break;
                case "dessert":
                    OrderedDessert dessert = new OrderedDessert();
                    dessert.setOrderId(orderId);
                    dessert.setDessertId(ci.getProductId());
                    dessert.setQuantity(ci.getQuantity());
                    dessert.setSize(ci.getSize());
                    orderedDessertDAO.insert(dessert);
                    break;
            }
        }

        // === 3. Сохраняем заказ в Firestore ===
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderId", orderId); // обязательно!
        orderMap.put("userId", order.getUserId());
        orderMap.put("userOrderNumber", order.getUserOrderNumber());
        orderMap.put("createdAt", order.getCreatedAt());
        orderMap.put("statusId", order.getStatusId());
        orderMap.put("totalPrice", order.getTotalPrice());
        orderMap.put("bonusDiscount", discountRub);
        orderMap.put("bonusUsed", bonusesToUse);

        Address address = addressDAO.getAddressByUserId(order.getUserId());
        if (address != null) {
            orderMap.put("address", address.getCity() + ", " + address.getStreet() + " " +
                    address.getHouse() + (address.getApartment().isEmpty() ? "" : ", кв. " + address.getApartment()));
        } else {
            orderMap.put("address", "");
        }

        final long bonusesToUseFinal = bonusesToUse;
        final long userBonusesFinal = userBonuses;
        final float totalAfterDiscountFinal = totalAfterDiscount;
        final long bonusEarnedFinal = (long) Math.floor(totalAfterDiscountFinal * 0.1f);
        final String userIdFinal = userId;

        firestore.collection("orders")
                .document(String.valueOf(orderId)) // Числовой id!
                .set(orderMap)
                .addOnSuccessListener(unused -> {
                    // После успешной отправки можно сохранять позиции
                    for (CartItem ci : items) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("productType", ci.getProductType());
                        itemMap.put("productId", ci.getProductId());
                        itemMap.put("title", ci.getTitle());
                        itemMap.put("imageUrl", ci.getImageUrl());
                        itemMap.put("size", ci.getSize());
                        itemMap.put("unitPrice", ci.getUnitPrice());
                        itemMap.put("quantity", ci.getQuantity());
                        firestore.collection("orders")
                                .document(String.valueOf(orderId))
                                .collection("ordered_items")
                                .add(itemMap);
                    }

                    // Списываем бонусы
                    if (bonusesToUseFinal > 0) {
                        FirebaseFirestore.getInstance()
                                .collection("users").document(userIdFinal)
                                .update("bonusPoints", userBonusesFinal - bonusesToUseFinal);
                    }

                    // Начисляем новые бонусы
                    if (bonusEarnedFinal > 0) {
                        FirebaseFirestore.getInstance()
                                .collection("users").document(userIdFinal)
                                .update("bonusPoints", userBonusesFinal - bonusesToUseFinal + bonusEarnedFinal);
                    }

                    // Очищаем корзину локально и на сервере
                    new Thread(() -> {
                        db.cartItemDao().clearAll();
                        clearCartAndCloud(userIdFinal);
                    }).start();

                    // Показываем тост: сколько списано и сколько начислено
                    final String msg = "Заказ оформлен"
                            + (bonusesToUseFinal > 0 ? "\nСписано бонусов: " + bonusesToUseFinal : "")
                            + (bonusEarnedFinal > 0 ? "\nНачислено бонусов: " + bonusEarnedFinal : "");

                    new Handler(requireActivity().getMainLooper()).post(() -> {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        ((MainActivity) requireActivity()).getBottomNavigationView()
                                .setSelectedItemId(R.id.nav_orders);
                    });
                })
                .addOnFailureListener(e -> {
                    new Handler(requireActivity().getMainLooper()).post(() -> {
                        Toast.makeText(requireContext(),
                                "Ошибка при сохранении заказа в облако: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
    }





    private void loadUserBonuses() {
        String userId = AuthUtils.getLoggedInUserId(requireContext());
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get().addOnSuccessListener(doc -> {
                    if (doc.contains("bonusPoints")) {
                        Object bp = doc.get("bonusPoints");
                        if (bp instanceof Number) userBonuses = ((Number) bp).longValue();
                        tvAvailableBonuses.setText("Доступно бонусов: " + userBonuses);
                        updateTotalWithBonuses(); // <--- сразу обновляем отображение
                    }
                });
    }


    private void clearCartAndCloud(String userId) {
        CollectionReference col = firestore.collection("carts")
                .document(userId)
                .collection("items");
        col.get().addOnSuccessListener(snap -> {
            for (DocumentSnapshot d : snap.getDocuments()) {
                col.document(d.getId()).delete();
            }
        });
    }

    private void updateCheckoutState(int sum) {
        btnCheckout.setEnabled(sum > 0 && hasAddress);
    }

    private void showAddressInputDialog() {
        // 1) Инфлейтим layout диалога
        View dlg = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_address, null);
        final EditText etCity      = dlg.findViewById(R.id.etCity);
        final EditText etStreet    = dlg.findViewById(R.id.etStreet);
        final EditText etHouse     = dlg.findViewById(R.id.etHouse);
        final EditText etApartment = dlg.findViewById(R.id.etApartment);

        // 2) Подтягиваем текущий адрес из Room и префилл
        String uid = AuthUtils.getLoggedInUserId(requireContext());
        if (uid != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                Address existing = addressDAO.getAddressByUserId(uid);
                if (existing != null) {
                    requireActivity().runOnUiThread(() -> {
                        etCity.    setText(existing.getCity());
                        etStreet.  setText(existing.getStreet());
                        etHouse.   setText(existing.getHouse());
                        etApartment.setText(existing.getApartment());
                    });
                }
            });
        }

        // 3) Строим и показываем AlertDialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Редактировать адрес")
                .setView(dlg)
                .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // собираем введённые данные
                        String city      = etCity.getText().toString().trim();
                        String street    = etStreet.getText().toString().trim();
                        String house     = etHouse.getText().toString().trim();
                        String apartment = etApartment.getText().toString().trim();
                        // сохраняем и синхронизируем
                        saveAndSyncAddress(city, street, house, apartment);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}