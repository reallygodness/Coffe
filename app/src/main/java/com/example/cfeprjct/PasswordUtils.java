package com.example.cfeprjct;

import android.util.Base64;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtils {
    private static final int SALT_LENGTH = 16;      // длина соли в байтах
    private static final int ITERATIONS = 10000;      // число итераций
    private static final int KEY_LENGTH = 256;        // длина ключа (в битах)

    // Генерация соли
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    // Хэширование пароля с использованием PBKDF2
    public static String hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        // Сохраняем соль и хэш в виде строки, разделённой двоеточием: salt:hash
        String saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP);
        return saltBase64 + ":" + hashBase64;
    }

    // Проверка пароля: извлекаем соль из сохранённого значения, хэшируем введённый пароль и сравниваем
    public static boolean verifyPassword(String providedPassword, String storedPassword)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        String[] parts = storedPassword.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
        String providedHash = hashPassword(providedPassword, salt);
        return providedHash.equals(storedPassword);
    }
}
