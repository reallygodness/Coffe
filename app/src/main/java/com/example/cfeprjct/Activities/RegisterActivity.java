package com.example.cfeprjct.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.AuthUtils;
import com.example.cfeprjct.PhoneNumberTextWatcher;
import com.example.cfeprjct.R;
import com.example.cfeprjct.UserRepository;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextPassword, firstNameEditText, lastNameEditText, emailEditText, phoneEditText, passwordEditText;
    private TextView requirementLength, requirementUpperLower, requirementDigit, requirementSpecial, requirementNoSpaces;
    private TextView link_to_login;
    private Button buttonRegister;
    private AppDatabase db;
    private UserRepository userRepository;
    private ImageView togglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextPassword = findViewById(R.id.password);
        requirementLength = findViewById(R.id.requirementLength);
        requirementUpperLower = findViewById(R.id.requirementUpperLower);
        requirementDigit = findViewById(R.id.requirementDigit);
        requirementSpecial = findViewById(R.id.requirementSpecial);
        requirementNoSpaces = findViewById(R.id.requirementNoSpaces);
        buttonRegister = findViewById(R.id.btnregister);

        firstNameEditText = findViewById(R.id.firstName);
        lastNameEditText = findViewById(R.id.lastName);
        emailEditText = findViewById(R.id.email);
        phoneEditText = findViewById(R.id.phoneNumber);
        passwordEditText = findViewById(R.id.password);
        togglePassword = findViewById(R.id.togglePassword);
        link_to_login = findViewById(R.id.loginLink);

        // Инициализируем репозиторий и базу
        userRepository = new UserRepository(this);
        db = AppDatabase.getInstance(this);

        // Добавляем маску для телефона
        phoneEditText.addTextChangedListener(new PhoneNumberTextWatcher(phoneEditText));

        // Переход к экрану авторизации
        link_to_login.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Обновляем требования к паролю при вводе
        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updatePasswordValidation(s.toString());
            }
        });

        buttonRegister.setOnClickListener(view -> {
            String password = editTextPassword.getText().toString().trim();
            if (!isValidPassword(password)) {
                Toast.makeText(RegisterActivity.this, "Пароль не соответствует требованиям", Toast.LENGTH_LONG).show();
                return;
            }
            // Здесь передаём пароль в открытом виде – UserRepository выполнит хэширование
            registerUser();
        });

        togglePassword.setOnClickListener(view -> {
            if (isPasswordVisible) {
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_eye);
            }
            isPasswordVisible = !isPasswordVisible;
            passwordEditText.setSelection(passwordEditText.getText().length());
        });
    }

    private void updatePasswordValidation(String password) {
        // Длина пароля
        if (password.length() >= 8 && password.length() <= 128) {
            requirementLength.setTextColor(Color.GREEN);
        } else {
            requirementLength.setTextColor(Color.RED);
        }
        // Заглавная и строчная буква
        if (password.matches(".*[a-z].*") && password.matches(".*[A-Z].*")) {
            requirementUpperLower.setTextColor(Color.GREEN);
        } else {
            requirementUpperLower.setTextColor(Color.RED);
        }
        // Хотя бы одна цифра
        if (password.matches(".*\\d.*")) {
            requirementDigit.setTextColor(Color.GREEN);
        } else {
            requirementDigit.setTextColor(Color.RED);
        }
        // Хотя бы один спецсимвол
        if (password.matches(".*[@#$%^&+=!].*")) {
            requirementSpecial.setTextColor(Color.GREEN);
        } else {
            requirementSpecial.setTextColor(Color.RED);
        }
        // Без пробелов
        if (!password.contains(" ")) {
            requirementNoSpaces.setTextColor(Color.GREEN);
        } else {
            requirementNoSpaces.setTextColor(Color.RED);
        }
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8 && password.length() <= 128 &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[@#$%^&+=!].*") &&
                !password.contains(" ");
    }

    private void registerUser() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phoneNumber = phoneEditText.getText().toString().trim().replaceAll("\\D", "");
        String password = passwordEditText.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                phoneNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!AuthUtils.isValidEmail(email)) {
            Toast.makeText(this, "Введите корректный email!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phoneNumber.startsWith("7") || phoneNumber.length() != 11) {
            Toast.makeText(this, "Введите корректный номер телефона!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(RegisterActivity.this, "Пароль не соответствует требованиям", Toast.LENGTH_LONG).show();
            return;
        }

        // Вызываем метод регистрации с 6 аргументами:
        // firstName, lastName, email, phoneNumber, password (plain text – в репозитории он будет хэширован),
        // и обратный вызов (AuthCallback)
        userRepository.registerUser(firstName, lastName, email, phoneNumber, password, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() ->
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
