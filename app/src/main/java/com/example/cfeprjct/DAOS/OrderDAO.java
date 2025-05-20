package com.example.cfeprjct.DAOS;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.Order;

import java.util.List;

@Dao
public interface OrderDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrder(Order order);

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    LiveData<List<Order>> getAllLiveOrders();

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    List<Order> getAllSync();

    @Query("DELETE FROM orders")
    void clearAll();


}