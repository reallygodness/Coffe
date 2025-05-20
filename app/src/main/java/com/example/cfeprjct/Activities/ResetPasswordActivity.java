package com.example.cfeprjct.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.R;
import com.example.cfeprjct.User;
import com.example.cfeprjct.UserDAO;
import com.example.cfeprjct.AuthUtils;
import com.example.cfeprjct.PasswordUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText newPassEditText;
    private EditText confirmPassEditText;
    private Button   resetButton;

    private String userId;
    private UserDAO userDAO;
    private FirebaseFirestore firestore;

    private TextView      reqLength, reqUpperLower, reqDigit, reqSpecial, reqNoSpaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password_acitvity);

        newPassEditText     = findViewById(R.id.newPasswordEditText);
        confirmPassEditText = findViewById(R.id.confirmPasswordEditText);
        resetButton         = findViewById(R.id.resetPasswordButton);
        reqLength          = findViewById(R.id.requirementLength);
        reqUpperLower      = findViewById(R.id.requirementUpperLower);
        reqDigit           = findViewById(R.id.requirementDigit);
        reqSpecial         = findViewById(R.id.requirementSpecial);
        reqNoSpaces        = findViewById(R.id.requirementNoSpaces);

        firestore = FirebaseFirestore.getInstance();
        userDAO   = AppDatabase.getInstance(this).userDAO();

        // Получаем userId из интента
        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this,
                    "Неизвестный пользователь",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2) Added: вешаем TextWatcher на поле нового пароля, чтобы динамически обновлять критерии
        newPassEditText.addTextChangedListener(passwordWatcher);

        resetButton.setOnClickListener(v -> attemptReset());
    }

    private final TextWatcher passwordWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int a) {}

        @Override public void afterTextChanged(Editable s) {
            String pwd = s.toString();

            // длина от 8 до 128
            boolean okLength = pwd.length() >= 8 && pwd.length() <= 128;
            updateRequirement(reqLength, okLength);

            // есть заглавная и строчная
            boolean okUpperLower = Pattern.compile("(?=.*[a-z])(?=.*[A-Z])").matcher(pwd).find();
            updateRequirement(reqUpperLower, okUpperLower);

            // хотя бы одна цифра
            boolean okDigit = Pattern.compile("(?=.*\\d)").matcher(pwd).find();
            updateRequirement(reqDigit, okDigit);

            // хотя бы один спецсимвол
            boolean okSpecial = Pattern.compile("(?=.*[!@#$%^&*+=])").matcher(pwd).find();
            updateRequirement(reqSpecial, okSpecial);

            // без пробелов
            boolean okNoSpaces = !pwd.contains(" ");
            updateRequirement(reqNoSpaces, okNoSpaces);
        }
    };

    // ↓↓↓ Added: метод, который меняет цвет требования в зависимости от выполнения
    private void updateRequirement(TextView tv, boolean isOk) {
        int color = ContextCompat.getColor(this,
                isOk ? R.color.green : R.color.red);
        tv.setTextColor(color);
    }

    private void attemptReset() {
        String p1 = newPassEditText.getText().toString().trim();
        String p2 = confirmPassEditText.getText().toString().trim();

        if (p1.isEmpty() || p2.isEmpty()) {
            Toast.makeText(this,
                    "Введите и подтвердите новый пароль",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!p1.equals(p2)) {
            Toast.makeText(this,
                    "Пароли не совпадают",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // проверим, что все критерии зелёные (по цвету текста)
        if (!allRequirementsMet()) {
            Toast.makeText(this, "Пароль не соответствует всем требованиям", Toast.LENGTH_SHORT).show();
            return;
        }

        // Хэшируем и сохраняем в фоне
        new Thread(() -> {
            try {
                byte[] salt = PasswordUtils.generateSalt();
                String hashed = PasswordUtils.hashPassword(p1, salt);

                // 1) Обновляем в локальной БД
                User user = userDAO.getUserById(userId);
                if (user != null) {
                    user.setPassword(hashed);
                    userDAO.updateUser(user);
                }

                // 2) Обновляем в Firestore
                firestore.collection("users")
                        .document(userId)
                        .update("password", hashed)
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                            Toast.makeText(this,
                                    "Пароль успешно обновлён",
                                    Toast.LENGTH_LONG).show();
                            // Возвращаемся на экран входа
                            Intent i = new Intent(this, LoginActivity.class);
                            startActivity(i);
                            finish();
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() ->
                                Toast.makeText(this,
                                        "Ошибка при обновлении пароля: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show()
                        ));

            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Ошибка при хэшировании пароля",
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    // ↓↓↓ Added: проверяем, что все пять требований выполнены
    private boolean allRequirementsMet() {
        return textIsGreen(reqLength)
                && textIsGreen(reqUpperLower)
                && textIsGreen(reqDigit)
                && textIsGreen(reqSpecial)
                && textIsGreen(reqNoSpaces);
    }

    private boolean textIsGreen(TextView tv) {
        int green = ContextCompat.getColor(this, R.color.green);
        return tv.getCurrentTextColor() == green;
    }
}