package com.example.cfeprjct.Activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.cfeprjct.Activities.Fragments.AdminCatalogFragment;
import com.example.cfeprjct.Activities.Fragments.AdminProfileFragment;
import com.example.cfeprjct.Activities.Fragments.AdminReportsFragment;
import com.example.cfeprjct.Activities.Fragments.ProfileFragment;
import com.example.cfeprjct.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        BottomNavigationView navView = findViewById(R.id.admin_bottom_nav);

        // По умолчанию открываем каталог
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.admin_fragment_container, new AdminCatalogFragment())
                    .commit();
            navView.setSelectedItemId(R.id.admin_catalog); // <--- ДОбавь ЭТУ СТРОКУ!
        }

        navView.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.admin_profile) {
                fragment = new ProfileFragment();
            } else if (id == R.id.admin_catalog) {
                fragment = new AdminCatalogFragment();
            } else if (id == R.id.admin_reports) {
                fragment = new AdminReportsFragment();
            } else {
                return false;
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.admin_fragment_container, fragment)
                    .commit();
            return true;
        });
    }
}