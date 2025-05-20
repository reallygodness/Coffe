package com.example.cfeprjct.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.R;
import com.example.cfeprjct.User;
import com.example.cfeprjct.UserDAO;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class VerifyCodeActivity extends AppCompatActivity {

    private EditText codeEditText;
    private Button verifyButton;
    private FirebaseFirestore firestore;

    // теперь точно не null, потому что мы их кладём в Intent
    private String userId;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        codeEditText  = findViewById(R.id.codeEditText);
        verifyButton  = findViewById(R.id.verifyButton);
        firestore     = FirebaseFirestore.getInstance();

        // получаем из Intent оба параметра:
        userId = getIntent().getStringExtra("userId");
        email  = getIntent().getStringExtra("email");

        // Если по какой-то причине userId не передался, сразу выходим
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this,
                    "Неизвестный пользователь для сброса пароля",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        verifyButton.setOnClickListener(v -> verifyCode());
    }

    private void verifyCode() {
        String enteredCode = codeEditText.getText().toString().trim();
        if (enteredCode.isEmpty()) {
            Toast.makeText(this, "Введите код подтверждения", Toast.LENGTH_SHORT).show();
            return;
        }

        // идём за документом password_resets/{userId}
        firestore.collection("password_resets")
                .document(userId)
                .get()
                .addOnSuccessListener(this::onCodeDocument)
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Ошибка проверки кода: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void onCodeDocument(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this,
                    "Код не найден. Пожалуйста, запросите новый.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String code = doc.getString("code");
        Long   ts   = doc.getLong("timestamp");
        if (code == null || ts == null) {
            Toast.makeText(this,
                    "Неверный формат кода",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверяем совпадение и время жизни (10 минут)
        if (!code.equals(codeEditText.getText().toString().trim())) {
            Toast.makeText(this, "Неверный код", Toast.LENGTH_SHORT).show();
            return;
        }
        if (System.currentTimeMillis() - ts > 10 * 60_000) {
            Toast.makeText(this, "Код устарел", Toast.LENGTH_SHORT).show();
            return;
        }

        // Всё ок — переходим к экрану смены пароля
        Intent i = new Intent(this, ResetPasswordActivity.class);
        i.putExtra("userId", userId);
        startActivity(i);
        finish();
    }
}