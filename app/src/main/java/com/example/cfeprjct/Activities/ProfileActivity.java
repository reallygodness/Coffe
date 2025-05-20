package com.example.cfeprjct.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.AuthUtils;
import com.example.cfeprjct.R;
import com.example.cfeprjct.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView firstNameTextView, lastNameTextView, emailTextView, phoneNumberTextView;
    private EditText firstNameEditText, lastNameEditText, emailEditText, phoneNumberEditText;
    private Button saveButton, editProfileButton, logoutButton;
    private ImageView profileImageView;
    private AppDatabase db;
    private String phoneNumber;
    private boolean isEditing = false;
    private byte[] selectedImageBytes = null;
    private ListenerRegistration userListener;
    private FirebaseFirestore firestore;

    // Важно: добавим userId
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        firestore = FirebaseFirestore.getInstance();

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app_database")
                .allowMainThreadQueries()
                .build();

        emailTextView = findViewById(R.id.emailTextView);
        phoneNumberTextView = findViewById(R.id.phoneNumberTextView);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        saveButton = findViewById(R.id.saveButton);
        editProfileButton = findViewById(R.id.editProfileButton);
        profileImageView = findViewById(R.id.profileImageView);
        logoutButton = findViewById(R.id.logoutButton);

        userId = AuthUtils.getLoggedInUserId(this);

        updateUI();

        profileImageView.setEnabled(false);
        profileImageView.setOnClickListener(v -> openGallery());
    }

    private void updateUI() {
        if (userId != null) {
            new Thread(() -> {
                User user = db.userDAO().getUserById(userId);
                if (user != null) {
                    runOnUiThread(() -> updateUIWithUser(user));
                }
            }).start();

            subscribeToFirestoreUser(userId); // тоже обновим ниже
        }
    }

    private void updateUIWithUser(User user) {
        emailTextView.setText("Email: " + user.getEmail());
        phoneNumberTextView.setText("Номер телефона: +" + user.getPhoneNumber());

        firstNameEditText.setText(user.getFirstName());
        lastNameEditText.setText(user.getLastName());
        emailEditText.setText(user.getEmail());
        phoneNumberEditText.setText(user.getPhoneNumber());

        if (user.getProfileImage() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(user.getProfileImage(), 0, user.getProfileImage().length);
            profileImageView.setImageBitmap(bitmap);
        } else {
            profileImageView.setImageResource(R.drawable.grayprofile);
        }
    }

    public void saveProfileChanges(View view) {
        // Получаем новый номер телефона из поля ввода
        String newPhoneNumber = phoneNumberEditText.getText().toString().trim();

        if (newPhoneNumber.isEmpty()) {
            Toast.makeText(this, "Введите номер телефона!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            // Получаем пользователя из локальной базы по userId
            User user = db.userDAO().getUserById(userId);
            if (user != null) {
                // Обновляем данные пользователя
                user.setPhoneNumber(newPhoneNumber);
                user.setFirstName(firstNameEditText.getText().toString().trim());
                user.setLastName(lastNameEditText.getText().toString().trim());
                user.setEmail(emailEditText.getText().toString().trim());
                if (selectedImageBytes != null) {
                    user.setProfileImage(selectedImageBytes);
                    selectedImageBytes = null;
                }

                // Сохраняем обновлённого пользователя в Room
                db.userDAO().insertUser(user);

                // Готовим обновлённые данные для Firestore
                Map<String, Object> updatedUserMap = new HashMap<>();
                updatedUserMap.put("userId", user.getUserId());
                updatedUserMap.put("phoneNumber", newPhoneNumber);
                updatedUserMap.put("firstName", user.getFirstName());
                updatedUserMap.put("lastName", user.getLastName());
                updatedUserMap.put("email", user.getEmail());
                if (user.getProfileImage() != null) {
                    updatedUserMap.put("profileImage", Base64.encodeToString(user.getProfileImage(), Base64.DEFAULT));
                }

                // Обновляем документ Firestore по userId (ключ не изменяется)
                firestore.collection("users").document(user.getUserId())
                        .set(updatedUserMap)
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                            // Обновляем отображение номера на экране
                            phoneNumberTextView.setText("Номер телефона: +" + newPhoneNumber);
                            updateUI();
                            Toast.makeText(ProfileActivity.this, "Профиль обновлён!", Toast.LENGTH_SHORT).show();
                            exitEditMode();

                            // Если номер изменён по сравнению с предыдущим, принудительно разлогинить пользователя,
                            // затем перезапускаем приложение – таким образом, при следующем запуске загружаются новые данные
                            if (!newPhoneNumber.equals(phoneNumber)) {
                                restartAppIfPhoneChanged(true);
                            } else {
                                restartAppIfPhoneChanged(false);
                            }
                        }))
                        .addOnFailureListener(e -> Log.e("Firestore", "❌ Ошибка обновления данных в Firestore", e));
            }
        }).start();
    }



    private void updateUserProfile(User user, String newPhoneNumber) {
        new Thread(() -> {
            String userId = user.getUserId(); // ✅ используем userId

            // Удалим старого пользователя, если номер изменился
            if (!phoneNumber.equals(newPhoneNumber)) {
                db.userDAO().deleteUserById(userId);
            }

            // Обновим локально
            db.userDAO().insertUser(user);



            // Готовим данные для Firestore
            Map<String, Object> updatedUser = new HashMap<>();
            updatedUser.put("userId", userId);
            updatedUser.put("phoneNumber", newPhoneNumber);
            updatedUser.put("firstName", user.getFirstName());
            updatedUser.put("lastName", user.getLastName());
            updatedUser.put("email", user.getEmail());

            if (user.getProfileImage() != null) {
                updatedUser.put("profileImage", Base64.encodeToString(user.getProfileImage(), Base64.DEFAULT));
            }

            // Firestore: если номер изменился, удалить старый документ
            if (!phoneNumber.equals(newPhoneNumber)) {
                firestore.collection("users").document(userId).delete()
                        .addOnSuccessListener(aVoid -> {
                            firestore.collection("users").document(userId)
                                    .set(updatedUser)
                                    .addOnSuccessListener(aVoid1 -> runOnUiThread(() -> {
                                        phoneNumber = newPhoneNumber;
                                        phoneNumberTextView.setText("Номер телефона: +" + newPhoneNumber);
                                        updateUI();
                                        Toast.makeText(ProfileActivity.this, "Профиль обновлён!", Toast.LENGTH_SHORT).show();
                                        exitEditMode();
                                    }))
                                    .addOnFailureListener(e -> Log.e("Firestore", "❌ Ошибка создания нового документа", e));
                        })
                        .addOnFailureListener(e -> Log.e("Firestore", "❌ Не удалось удалить старый документ", e));
            } else {
                // Просто обновим текущий документ
                firestore.collection("users").document(userId)
                        .set(updatedUser)
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                            phoneNumberTextView.setText("Номер телефона: +" + newPhoneNumber);
                            updateUI();
                            Toast.makeText(ProfileActivity.this, "Профиль обновлён!", Toast.LENGTH_SHORT).show();
                            exitEditMode();
                        }))
                        .addOnFailureListener(e -> Log.e("Firestore", "❌ Ошибка обновления", e));
            }

        }).start();
    }

    private void restartAppIfPhoneChanged(boolean forceLogout) {
        runOnUiThread(() -> {
            if (forceLogout) {
                // Принудительный выход: очищаем сохранённые данные авторизации
                AuthUtils.clearLogin(ProfileActivity.this);
            }
            // Перезапускаем приложение, переходя на экран WelcomeActivity
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }




    private void subscribeToFirestoreUser(String id) {
        if (id == null) return;

        if (userListener != null) {
            userListener.remove();
        }

        userListener = firestore.collection("users").document(id)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "❌ Ошибка при получении данных", error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String email = documentSnapshot.getString("email");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        String profileImageBase64 = documentSnapshot.getString("profileImage");

                        User firestoreUser = new User(userId, firstName, lastName, email, phoneNumber);
                        if (profileImageBase64 != null) {
                            firestoreUser.setProfileImage(Base64.decode(profileImageBase64, Base64.DEFAULT));
                        }

                        updateUIWithUser(firestoreUser);
                    } else {
                        new Thread(() -> {
                            db.userDAO().deleteUserById(id);
                            runOnUiThread(() -> {
                                Toast.makeText(ProfileActivity.this, "Профиль удалён. Повторный вход необходим.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                        }).start();
                    }
                });
    }




    private void exitEditMode() {
        isEditing = false;
        editProfileButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.VISIBLE);

        firstNameEditText.setVisibility(View.GONE);
        lastNameEditText.setVisibility(View.GONE);
        emailEditText.setVisibility(View.GONE);
        phoneNumberEditText.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

        profileImageView.setEnabled(false);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private final androidx.activity.result.ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            saveImageToDatabase(imageUri);
                        }
                    });

    private void saveImageToDatabase(Uri imageUri) {
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            selectedImageBytes = outputStream.toByteArray();

            profileImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
