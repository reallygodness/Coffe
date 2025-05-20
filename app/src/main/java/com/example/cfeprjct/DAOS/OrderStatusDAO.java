package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.OrderStatus;

import java.util.List;

@Dao
public interface OrderStatusDAO {
        @Query("SELECT * FROM order_statuses")
        List<OrderStatus> getAllStatuses();
        /** Вариант с varargs — можно передавать сразу несколько статусов */
        @Insert(onConflict = OnConflictStrategy.IGNORE)
        long insert(OrderStatus status);

        @Query("SELECT * FROM order_statuses WHERE statusName = :name LIMIT 1")
        OrderStatus getByName(String name);

        @Query("SELECT * FROM order_statuses WHERE statusId = :id")
        OrderStatus getById(int id);

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        void insertAll(OrderStatus... statuses);
    }

