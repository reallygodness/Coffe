package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.Delivery;

import java.util.List;

@Dao
public interface DeliveryDAO {

    @Insert
    long insertDelivery(Delivery delivery);

    @Update
    void updateDelivery(Delivery delivery);

    @Delete
    void deleteDelivery(Delivery delivery);

    @Query("SELECT * FROM deliveries WHERE deliveryId = :deliveryId")
    Delivery getDeliveryById(int deliveryId);

    @Query("SELECT * FROM deliveries WHERE orderId = :orderId")
    List<Delivery> getDeliveriesByOrderId(int orderId);
}