package com.example.cfeprjct.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cfeprjct.AuthUtils;
import com.example.cfeprjct.R;

public class WelcomeActivity extends AppCompatActivity {

    private Button btnstart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);
        btnstart = findViewById(R.id.buttonStart);

        btnstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent( WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        // Проверяем, авторизован ли пользователь
        if (AuthUtils.isLoggedIn(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
