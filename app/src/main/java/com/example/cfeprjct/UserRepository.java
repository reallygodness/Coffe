package com.example.cfeprjct;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class UserRepository {

    private final AppDatabase db;
    private final UserDAO     userDao;
    private final FirebaseFirestore firestore;

    public interface AuthCallback {
        void onSuccess(User user);         // <-- теперь принимаем объект User
        void onFailure(String errorMessage);
    }

    public UserRepository(Context context) {
        db       = AppDatabase.getInstance(context.getApplicationContext());
        userDao  = db.userDAO();
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Регистрация пользователя.
     * Пароль хэшируется через PBKDF2, а userId генерируется
     * автоматически Firestore при создании нового документа.
     */
    public void registerUser(String firstName,
                             String lastName,
                             String email,
                             String phone,
                             String password,
                             AuthCallback callback) {

        new Thread(() -> {
            if (userDao.getUserByEmail(email) != null) {
                callback.onFailure("Пользователь с таким email уже есть");
                return;
            }
            if (userDao.getUserByPhoneNumber(phone) != null) {
                callback.onFailure("Пользователь с таким номером уже есть");
                return;
            }

            firestore.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(emailSnap -> {
                        if (!emailSnap.isEmpty()) {
                            callback.onFailure("Email уже используется в другом аккаунте!");
                            return;
                        }

                        firestore.collection("users")
                                .whereEqualTo("phoneNumber", phone)
                                .get()
                                .addOnSuccessListener(phoneSnap -> {
                                    if (!phoneSnap.isEmpty()) {
                                        callback.onFailure("Номер телефона уже используется в другом аккаунте!");
                                        return;
                                    }

                                    try {
                                        byte[] salt = PasswordUtils.generateSalt();
                                        String hashedPassword = PasswordUtils.hashPassword(password, salt);

                                        // Генерируем userId заранее (этот id будет id документа)
                                        DocumentReference newUserRef = firestore.collection("users").document();
                                        String generatedId = newUserRef.getId();

                                        User newUser = new User(firstName, lastName, email, phone, hashedPassword);
                                        newUser.setUserId(generatedId);
                                        newUser.setRoleId(1); // роль user

                                        // Сохраняем локально
                                        new Thread(() -> userDao.insertUser(newUser)).start();

                                        // Firestore: теперь userId — это имя документа!
                                        Map<String, Object> userMap = new HashMap<>();
                                        userMap.put("userId", generatedId);
                                        userMap.put("firstName", firstName);
                                        userMap.put("lastName", lastName);
                                        userMap.put("email", email);
                                        userMap.put("phoneNumber", phone);
                                        userMap.put("password", hashedPassword);
                                        userMap.put("roleId", 1);

                                        if (newUser.getProfileImage() != null) {
                                            String b64 = Base64.encodeToString(newUser.getProfileImage(), Base64.DEFAULT);
                                            userMap.put("profileImage", b64);
                                        }

                                        // !!! Вот это главное изменение: .document(generatedId)
                                        firestore.collection("users")
                                                .document(generatedId)
                                                .set(userMap)
                                                .addOnSuccessListener(aVoid -> callback.onSuccess(newUser))
                                                .addOnFailureListener(e ->
                                                        callback.onFailure("Ошибка создания аккаунта в облаке: " + e.getMessage()));

                                    } catch (Exception ex) {
                                        callback.onFailure("Ошибка хэширования пароля: " + ex.getMessage());
                                    }
                                })
                                .addOnFailureListener(e -> callback.onFailure("Ошибка проверки телефона: " + e.getMessage()));
                    })
                    .addOnFailureListener(e -> callback.onFailure("Ошибка проверки email: " + e.getMessage()));
        }).start();
    }


    /**
     * Авторизация пользователя. Сначала локально, иначе — из Firestore.
     */
    public void loginUser(String phoneNumber,
                          String password,
                          AuthCallback callback) {
        new Thread(() -> {
            // Локальная попытка
            User local = userDao.getUserByPhoneNumber(phoneNumber);
            if (local != null) {
                try {
                    if (PasswordUtils.verifyPassword(password, local.getPassword())) {
                        if (local.getRoleId() == 0) local.setRoleId(1); // На всякий случай!
                        callback.onSuccess(local);
                    } else {
                        callback.onFailure("Неверный номер или пароль");
                    }
                } catch (Exception e) {
                    callback.onFailure("Ошибка проверки пароля: " + e.getMessage());
                }
                return;
            }

            // Поиск в Firestore
            firestore.collection("users")
                    .whereEqualTo("phoneNumber", phoneNumber)
                    .get()
                    .addOnSuccessListener(qs -> {
                        if (qs.isEmpty()) {
                            callback.onFailure("Пользователь не найден");
                            return;
                        }
                        DocumentReference doc = qs.getDocuments().get(0).getReference();
                        doc.get().addOnSuccessListener(snapshot -> {
                            String storedHash = snapshot.getString("password");
                            try {
                                if (!PasswordUtils.verifyPassword(password, storedHash)) {
                                    callback.onFailure("Неверный номер или пароль");
                                    return;
                                }
                                // Собираем объект
                                User cloudUser = new User();
                                cloudUser.setUserId(snapshot.getString("userId"));
                                cloudUser.setFirstName(snapshot.getString("firstName"));
                                cloudUser.setLastName(snapshot.getString("lastName"));
                                cloudUser.setEmail(snapshot.getString("email"));
                                cloudUser.setPhoneNumber(phoneNumber);
                                cloudUser.setPassword(storedHash);

                                // Получаем roleId из Firestore (или по умолчанию 1)
                                Long role = snapshot.getLong("roleId");
                                cloudUser.setRoleId(role != null ? role.intValue() : 1);

                                String pi = snapshot.getString("profileImage");
                                if (pi != null) {
                                    cloudUser.setProfileImage(Base64.decode(pi, Base64.DEFAULT));
                                }

                                // Сохраняем локально
                                new Thread(() -> userDao.insertUser(cloudUser)).start();

                                callback.onSuccess(cloudUser);

                            } catch (Exception ex) {
                                callback.onFailure("Ошибка проверки пароля: " + ex.getMessage());
                            }
                        }).addOnFailureListener(e ->
                                callback.onFailure("Ошибка чтения из облака: " + e.getMessage()));
                    })
                    .addOnFailureListener(e ->
                            callback.onFailure("Ошибка соединения: " + e.getMessage()));
        }).start();
    }

    public void updateUserProfile(User user, AuthCallback callback) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstName", user.getFirstName());
        userMap.put("lastName", user.getLastName());
        userMap.put("email", user.getEmail());
        userMap.put("phoneNumber", user.getPhoneNumber());
        // ОБЯЗАТЕЛЬНО сохраняем roleId!
        userMap.put("roleId", user.getRoleId());
        if (user.getProfileImage() != null) {
            String b64 = android.util.Base64.encodeToString(user.getProfileImage(), android.util.Base64.DEFAULT);
            userMap.put("profileImage", b64);
        }

        firestore.collection("users")
                .document(user.getUserId())
                .update(userMap)
                .addOnSuccessListener(aVoid -> {
                    // Локально синхронизируем
                    new Thread(() -> userDao.insertUser(user)).start();
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> callback.onFailure("Ошибка обновления профиля: " + e.getMessage()));
    }

}
