package com.example.cfeprjct;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Patterns;

import java.util.Random;

public class AuthUtils {
    private static final String PREF_NAME = "user_preferences";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_Id"; // Храним только userId

    // Сохраняем состояние входа и userId
    public static void setLoggedIn(Context context, boolean isLoggedIn, String userId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        if (isLoggedIn) {
            editor.putString(KEY_USER_ID, userId);
        } else {
            editor.remove(KEY_USER_ID);
        }
        editor.apply();
    }

    // Получаем сохранённый userId
    public static String getLoggedInUserId(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_ID, null);
    }

    // Проверяем, авторизован ли пользователь
    public static boolean isLoggedIn(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Очищает данные авторизации
    public static void clearLogin(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    // Проверка email
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Генерация кода для восстановления пароля
    public static String generateResetCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
