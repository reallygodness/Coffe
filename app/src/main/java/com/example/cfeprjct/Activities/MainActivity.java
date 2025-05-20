package com.example.cfeprjct.Activities;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.cfeprjct.Activities.Fragments.CatalogFragment;
import com.example.cfeprjct.Activities.Fragments.CartFragment;
import com.example.cfeprjct.Activities.Fragments.OrdersFragment;
import com.example.cfeprjct.Activities.Fragments.ProfileFragment;
import com.example.cfeprjct.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Отключаем ночной режим
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        // Edge-to-edge
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;

            int id = item.getItemId();
            if (id == R.id.nav_catalog) {
                selectedFragment = new CatalogFragment();
            } else if (id == R.id.nav_cart) {
                selectedFragment = new CartFragment();
            } else if (id == R.id.nav_orders) {
                selectedFragment = new OrdersFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else {
                return false;
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_container, selectedFragment)
                    .commit();

            return true;
        });

        // При первом запуске сразу открываем каталог
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_catalog);
        }
    }

    /**
     * Чтобы из фрагментов было удобно переключать пункт BottomNavigationView,
     * возвращаем здесь его экземпляр.
     */
    public BottomNavigationView getBottomNavigationView() {
        return findViewById(R.id.bottom_navigation);
    }
}
