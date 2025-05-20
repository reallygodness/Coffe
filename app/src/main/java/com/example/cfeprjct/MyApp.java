package com.example.cfeprjct;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import androidx.room.Room;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.cfeprjct.Sync.CatalogSync;
import com.example.cfeprjct.Sync.SyncUserWorker;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyApp extends Application {
    private FirebaseFirestore firestore;
    private AppDatabase localDb;

    @Override
    public void onCreate() {
        super.onCreate();
        CatalogSync sync = new CatalogSync(this);
        // Просто ставим в очередь однократную задачу синхронизации:
        OneTimeWorkRequest syncReq = new OneTimeWorkRequest.Builder(SyncUserWorker.class)
                .build();
        WorkManager.getInstance(this).enqueue(syncReq);
    }

}
