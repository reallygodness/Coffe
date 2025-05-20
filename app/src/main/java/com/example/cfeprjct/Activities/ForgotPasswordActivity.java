package com.example.cfeprjct.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.AuthUtils;
import com.example.cfeprjct.R;
import com.example.cfeprjct.User;
import com.example.cfeprjct.UserDAO;
import com.example.cfeprjct.api.EmailSender;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText emailEditText;
    private Button sendCodeButton;

    private FirebaseFirestore firestore;
    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailEditText  = findViewById(R.id.etEmail);
        sendCodeButton = findViewById(R.id.btnsendcode);

        firestore = FirebaseFirestore.getInstance();
        userDAO   = AppDatabase.getInstance(this).userDAO();

        sendCodeButton.setOnClickListener(v -> sendResetCode());
    }

    private void sendResetCode() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            User user = userDAO.getUserByEmail(email);
            if (user == null) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Пользователь с таким email не найден",
                                Toast.LENGTH_SHORT).show()
                );
                return;
            }

            // получаем userId из локального профиля
            String userId = user.getUserId();

            // генерируем 6-значный код
            String code = String.format(Locale.getDefault(),
                    "%06d", new Random().nextInt(1_000_000));

            Map<String,Object> data = new HashMap<>();
            data.put("code",      code);
            data.put("timestamp", System.currentTimeMillis());

            firestore.collection("password_resets")
                    .document(userId)      // документ по userId
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        // отправляем письмо
                        EmailSender.send(
                                email,
                                "Код сброса пароля",
                                "Ваш код для сброса пароля: " + code
                        );

                        runOnUiThread(() -> {
                            Toast.makeText(this,
                                    "Код отправлен на email",
                                    Toast.LENGTH_SHORT).show();

                            Intent i = new Intent(this, VerifyCodeActivity.class);
                            // передаём **оба** параметра:
                            i.putExtra("email",  email);
                            i.putExtra("userId", userId);
                            startActivity(i);
                            finish();
                        });
                    })
                    .addOnFailureListener(e -> runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Ошибка при отправке кода",
                                    Toast.LENGTH_SHORT).show()
                    ));

        }).start();
    }
}