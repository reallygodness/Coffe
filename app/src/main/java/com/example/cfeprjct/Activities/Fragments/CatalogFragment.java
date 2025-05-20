package com.example.cfeprjct.Activities.Fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cfeprjct.Adapters.CatalogAdapter;
import com.example.cfeprjct.Adapters.CatalogItem;
import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.AuthUtils;
import com.example.cfeprjct.DAOS.AddressDAO;
import com.example.cfeprjct.DAOS.DessertDAO;
import com.example.cfeprjct.DAOS.DishDAO;
import com.example.cfeprjct.DAOS.DrinkDAO;
import com.example.cfeprjct.DAOS.PriceListDAO;
import com.example.cfeprjct.Entities.Address;
import com.example.cfeprjct.Entities.Dessert;
import com.example.cfeprjct.Entities.Dish;
import com.example.cfeprjct.Entities.Drink;
import com.example.cfeprjct.R;
import com.example.cfeprjct.Sync.CatalogSync;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatalogFragment extends Fragment {
    private static final int REQ_LOC = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore         firestore;

    private AppDatabase     db;
    private DrinkDAO        drinkDAO;
    private DishDAO         dishDAO;
    private DessertDAO      dessertDAO;
    private PriceListDAO    priceListDAO;
    private AddressDAO      addressDAO;

    private EditText        searchEditText;
    private RecyclerView    catalogRecyclerView;
    private CatalogAdapter  catalogAdapter;
    private MaterialButton  drinkTabButton, dishTabButton, dessertTabButton;
    private TextView        addressTextView;
    private ImageView       editAddressButton;

    private SwipeRefreshLayout swipeRefreshLayout;

    private enum Category { DRINKS, DISHES, DESSERTS }
    private Category currentCategory = Category.DRINKS;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // статус-бар
        Window window = requireActivity().getWindow();
        window.setStatusBarColor(Color.BLACK);
        new WindowInsetsControllerCompat(window, window.getDecorView())
                .setAppearanceLightStatusBars(false);

        View view = inflater.inflate(R.layout.fragment_catalog, container, false);

        // Room & Firestore
        db             = AppDatabase.getInstance(requireContext());
        drinkDAO       = db.drinkDAO();
        dishDAO        = db.dishDAO();
        dessertDAO     = db.dessertDAO();
        priceListDAO   = db.priceListDAO();
        addressDAO     = db.addressDAO();
        firestore      = FirebaseFirestore.getInstance();

        // UI
        searchEditText      = view.findViewById(R.id.searchEditText);
        drinkTabButton      = view.findViewById(R.id.drinkTabButton);
        dishTabButton       = view.findViewById(R.id.dishTabButton);
        dessertTabButton    = view.findViewById(R.id.dessertTabButton);
        catalogRecyclerView = view.findViewById(R.id.catalogRecyclerView);
        addressTextView     = view.findViewById(R.id.addressTextView);
        editAddressButton   = view.findViewById(R.id.editAddressButton);
        // === ↓↓↓ добавлено Pull-to-Refresh ↓↓↓ ===
        swipeRefreshLayout  = view.findViewById(R.id.swipeRefreshLayout);
        // === ↑↑↑ добавлено Pull-to-Refresh ↑↑↑ ===

        catalogRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        catalogAdapter = new CatalogAdapter();
        catalogRecyclerView.setAdapter(catalogAdapter);

        // Загрузка адреса из Room или Firestore
        String userId = AuthUtils.getLoggedInUserId(requireContext());
        if (userId != null) {
            new Thread(() -> {
                Address local = addressDAO.getAddressByUserId(userId);
                if (local != null) {
                    String formatted = local.getCity() + ", "
                            + local.getStreet() + " "
                            + local.getHouse()
                            + (local.getApartment().isEmpty() ? "" : ", кв. " + local.getApartment());
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> addressTextView.setText(formatted));
                    }
                } else {
                    fetchAddressFromFirestore(userId);
                }
            }).start();
        }

        // Геолокация
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        addressTextView.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQ_LOC);
            } else {
                requestLastLocation();
            }
        });
        editAddressButton.setOnClickListener(v -> showAddressInputDialog());

        CatalogSync sync = new CatalogSync(requireContext());
        sync.syncPrices(() -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                updateTabStyles();
                String q = searchEditText.getText().toString().trim();
                switch (currentCategory) {
                    case DRINKS:   loadDrinks(q); break;
                    case DISHES:   loadDishes(q); break;
                    case DESSERTS: loadDesserts(q); break;
                }
            });
        });

        // Устанавливаем табы
        drinkTabButton.setOnClickListener(v -> {
            currentCategory = Category.DRINKS;
            updateTabStyles();
            loadDrinks(searchEditText.getText().toString());
        });
        dishTabButton.setOnClickListener(v -> {
            currentCategory = Category.DISHES;
            updateTabStyles();
            loadDishes(searchEditText.getText().toString());
        });
        dessertTabButton.setOnClickListener(v -> {
            currentCategory = Category.DESSERTS;
            updateTabStyles();
            loadDesserts(searchEditText.getText().toString());
        });

        // Поиск
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void onTextChanged    (CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged (Editable s) {
                String q = s.toString().trim();
                switch (currentCategory) {
                    case DRINKS:   loadDrinks  (q); break;
                    case DISHES:   loadDishes  (q); break;
                    case DESSERTS: loadDesserts(q); break;
                }
            }
        });

        // === ↓↓↓ добавлено Pull-to-Refresh ↓↓↓ ===
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // при свайпе вниз — синхронизируем именно активную категорию
            switch (currentCategory) {
                case DRINKS:
                    sync.syncDrinks(() -> runOnUiThreadIfAdded(() -> {
                        loadDrinks(searchEditText.getText().toString());
                        swipeRefreshLayout.setRefreshing(false);
                    }));
                    break;
                case DISHES:
                    sync.syncDishes(() -> runOnUiThreadIfAdded(() -> {
                        loadDishes(searchEditText.getText().toString());
                        swipeRefreshLayout.setRefreshing(false);
                    }));
                    break;
                case DESSERTS:
                    sync.syncDesserts(() -> runOnUiThreadIfAdded(() -> {
                        loadDesserts(searchEditText.getText().toString());
                        swipeRefreshLayout.setRefreshing(false);
                    }));
                    break;
            }
        });
        // === ↑↑↑ добавлено Pull-to-Refresh ↑↑↑ ===

        // Первоначальная стилизация и загрузка
        updateTabStyles();

        return view;
    }
    /** Программно меняет фон, текст и обводку кнопок в зависимости от selected */
    private void updateTabStyles() {
        // параметры
        int accent = Color.parseColor("#C67C4E");
        int strokeWidth = (int)(2 * getResources().getDisplayMetrics().density + .5f);
        // DRINKS
        if (currentCategory == Category.DRINKS) {
            drinkTabButton.setBackgroundTintList(ColorStateList.valueOf(accent));
            drinkTabButton.setTextColor(Color.WHITE);
            drinkTabButton.setStrokeWidth(0);
        } else {
            drinkTabButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            drinkTabButton.setTextColor(accent);
            drinkTabButton.setStrokeColor(ColorStateList.valueOf(accent));
            drinkTabButton.setStrokeWidth(strokeWidth);
        }
        // DISHES
        if (currentCategory == Category.DISHES) {
            dishTabButton.setBackgroundTintList(ColorStateList.valueOf(accent));
            dishTabButton.setTextColor(Color.WHITE);
            dishTabButton.setStrokeWidth(0);
        } else {
            dishTabButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            dishTabButton.setTextColor(accent);
            dishTabButton.setStrokeColor(ColorStateList.valueOf(accent));
            dishTabButton.setStrokeWidth(strokeWidth);
        }
        // DESSERTS
        if (currentCategory == Category.DESSERTS) {
            dessertTabButton.setBackgroundTintList(ColorStateList.valueOf(accent));
            dessertTabButton.setTextColor(Color.WHITE);
            dessertTabButton.setStrokeWidth(0);
        } else {
            dessertTabButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            dessertTabButton.setTextColor(accent);
            dessertTabButton.setStrokeColor(ColorStateList.valueOf(accent));
            dessertTabButton.setStrokeWidth(strokeWidth);
        }
    }

    private void runOnUiThreadIfAdded(Runnable r) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(r);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQ_LOC
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLastLocation();
        } else {
            Toast.makeText(requireContext(),
                    "Разрешение на геолокацию не получено",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestLastLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) reverseGeocode(loc.getLatitude(), loc.getLongitude());
                    else Toast.makeText(requireContext(),
                            "Не удалось получить локацию", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("CatalogFragment", "Ошибка локации", e);
                    Toast.makeText(requireContext(),
                            "Ошибка при получении геолокации", Toast.LENGTH_SHORT).show();
                });
    }

    private void reverseGeocode(double lat, double lng) {
        new Thread(() -> {
            try {
                Geocoder gc = new Geocoder(requireContext());
                List<android.location.Address> list = gc.getFromLocation(lat, lng, 1);
                if (list != null && !list.isEmpty()) {
                    android.location.Address src = list.get(0);
                    String city   = src.getLocality();
                    String street = src.getThoroughfare();
                    String house  = src.getFeatureName();
                    String formatted = city + ", " + street + " " + house;
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                addressTextView.setText(formatted));
                    }
                    saveAndSyncAddress(city, street, house, "");
                }
            } catch (IOException e) {
                Log.e("CatalogFragment", "reverseGeocode error", e);
            }
        }).start();
    }

    private void showAddressInputDialog() {
        View dlg = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_address, null);
        EditText etCity      = dlg.findViewById(R.id.etCity);
        EditText etStreet    = dlg.findViewById(R.id.etStreet);
        EditText etHouse     = dlg.findViewById(R.id.etHouse);
        EditText etApartment = dlg.findViewById(R.id.etApartment);

        // Подгружаем текущий адрес
        new Thread(() -> {
            String uid = AuthUtils.getLoggedInUserId(requireContext());
            if (uid != null) {
                Address existing = addressDAO.getAddressByUserId(uid);
                if (existing != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        etCity.    setText(existing.getCity());
                        etStreet.  setText(existing.getStreet());
                        etHouse.   setText(existing.getHouse());
                        etApartment.setText(existing.getApartment());
                    });
                }
            }
        }).start();

        new AlertDialog.Builder(requireContext())
                .setTitle("Редактировать адрес")
                .setView(dlg)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    saveAndSyncAddress(
                            etCity.getText().toString().trim(),
                            etStreet.getText().toString().trim(),
                            etHouse.getText().toString().trim(),
                            etApartment.getText().toString().trim()
                    );
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveAndSyncAddress(String city,
                                    String street,
                                    String house,
                                    String apartment) {
        String userId = AuthUtils.getLoggedInUserId(requireContext());
        if (userId == null) return;

        new Thread(() -> {
            Address existing = addressDAO.getAddressByUserId(userId);
            int addressId;
            if (existing != null) {
                existing.setCity(city);
                existing.setStreet(street);
                existing.setHouse(house);
                existing.setApartment(apartment);
                addressDAO.updateAddress(existing);
                addressId = existing.getAddressId();
            } else {
                Address addr = new Address();
                addr.setUserId(userId);
                addr.setCity(city);
                addr.setStreet(street);
                addr.setHouse(house);
                addr.setApartment(apartment);
                addressId = (int) addressDAO.insertAddress(addr);
            }

            Map<String,Object> map = new HashMap<>();
            map.put("addressId", addressId);
            map.put("userId",    userId);
            map.put("city",      city);
            map.put("street",    street);
            map.put("house",     house);
            map.put("apartment", apartment);

            firestore.collection("addresses")
                    .document(String.valueOf(addressId))
                    .set(map);

            firestore.collection("users")
                    .document(userId)
                    .update("addressId", addressId);

            String formatted = city
                    + ", " + street
                    + " "  + house
                    + (apartment.isEmpty() ? "" : ", кв. " + apartment);
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        addressTextView.setText(formatted));
            }
        }).start();
    }

    private void fetchAddressFromFirestore(String userId) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists() && userDoc.contains("addressId")) {
                        String addrId = String.valueOf(userDoc.getLong("addressId"));
                        firestore.collection("addresses")
                                .document(addrId)
                                .get()
                                .addOnSuccessListener(addrDoc -> {
                                    if (addrDoc.exists()) {
                                        String city   = addrDoc.getString("city");
                                        String street = addrDoc.getString("street");
                                        String house  = addrDoc.getString("house");
                                        String apt    = addrDoc.getString("apartment");
                                        String formatted = city
                                                + ", " + street
                                                + " " + house
                                                + (apt != null && !apt.isEmpty() ? ", кв. " + apt : "");
                                        if (isAdded()) {
                                            addressTextView.setText(formatted);
                                        }
                                        new Thread(() -> {
                                            Address a = new Address();
                                            a.setUserId(userId);
                                            a.setCity(city   != null ? city   : "");
                                            a.setStreet(street != null ? street : "");
                                            a.setHouse(house  != null ? house  : "");
                                            a.setApartment(apt != null ? apt    : "");
                                            addressDAO.insertAddress(a);
                                        }).start();
                                    }
                                });
                    }
                });
    }

    private void loadDrinks(String query) {
        new Thread(() -> {
            List<Drink> list = query.isEmpty()
                    ? drinkDAO.getAllDrinks()
                    : drinkDAO.searchDrinksByName("%" + query + "%");
            List<CatalogItem> items = new ArrayList<>();
            for (Drink d : list) {
                int price = (int) priceListDAO.getLatestPriceForDrink(d.getDrinkId());
                // size=0 для напитков
                CatalogItem item = new CatalogItem(
                        d.getDrinkId(),
                        d.getName(),
                        d.getDescription(),
                        price,
                        "drink",
                        d.getImageUrl(),
                        0
                );
                Float avg = db.reviewDAO().getAverageRatingForDrinkId(d.getDrinkId());
                item.setRating(avg != null ? avg : 0f);
                items.add(item);
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        catalogAdapter.setItems(items)
                );
            }
        }).start();
    }

    private void loadDishes(String query) {
        new Thread(() -> {
            List<Dish> list = query.isEmpty()
                    ? dishDAO.getAllDishes()
                    : dishDAO.searchDishesByName("%" + query + "%");
            List<CatalogItem> items = new ArrayList<>();
            for (Dish d : list) {
                int price = (int) priceListDAO.getLatestPriceForDish(d.getDishId());
                CatalogItem item = new CatalogItem(
                        d.getDishId(),
                        d.getName(),
                        d.getDescription(),
                        price,
                        "dish",
                        d.getImageUrl(),
                        d.getSize()
                );
                Float avg = db.reviewDAO().getAverageRatingForDishId(d.getDishId());
                item.setRating(avg != null ? avg : 0f);
                items.add(item);
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        catalogAdapter.setItems(items)
                );
            }
        }).start();
    }

    private void loadDesserts(String query) {
        new Thread(() -> {
            List<Dessert> list = query.isEmpty()
                    ? dessertDAO.getAllDesserts()
                    : dessertDAO.searchDessertsByName("%" + query + "%");
            List<CatalogItem> items = new ArrayList<>();
            for (Dessert d : list) {
                int price = (int) priceListDAO.getLatestPriceForDessert(d.getDessertId());
                CatalogItem item = new CatalogItem(
                        d.getDessertId(),
                        d.getName(),
                        d.getDescription(),
                        price,
                        "dessert",
                        d.getImageUrl(),
                        d.getSize()
                );
                Float avg = db.reviewDAO().getAverageRatingForDessertId(d.getDessertId());
                item.setRating(avg != null ? avg : 0f);
                items.add(item);
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        catalogAdapter.setItems(items)
                );
            }
        }).start();
    }
}
