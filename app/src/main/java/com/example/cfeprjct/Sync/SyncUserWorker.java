package com.example.cfeprjct.Sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import android.content.Context;
import android.util.Base64;
import android.util.Log;


import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;

public class SyncUserWorker extends Worker {
    private static final String TAG = "SyncUsersWorker";

    public SyncUserWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
    }

    @NonNull @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<User> users = db.userDAO().getAllUsers(); // это в фоне!
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        for (User u : users) {
            if (u.getUserId() == null || u.getUserId().isEmpty()) {
                Log.w(TAG, "Пропущен пользователь с пустым userId");
                continue;
            }
            Map<String,Object> m = new HashMap<>();
            m.put("userId", u.getUserId());
            m.put("firstName", u.getFirstName());
            m.put("lastName",  u.getLastName());
            m.put("email",     u.getEmail());
            m.put("phoneNumber", u.getPhoneNumber());
            if (u.getPassword() != null)       m.put("password",      u.getPassword());
            if (u.getProfileImage() != null) {
                String b64 = Base64.encodeToString(u.getProfileImage(), Base64.DEFAULT);
                m.put("profileImage", b64);
            }
            firestore.collection("users")
                    .document(u.getUserId())
                    .set(m)
                    .addOnSuccessListener(a -> Log.d(TAG, "✅ синхронизирован " + u.getUserId()))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ ошибка sync", e));
        }

        return Result.success();
    }
}
