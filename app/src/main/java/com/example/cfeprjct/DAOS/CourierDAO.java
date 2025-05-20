package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.Courier;

import java.util.List;

@Dao
public interface CourierDAO {

    @Insert
    long insertCourier(Courier courier);

    @Update
    void updateCourier(Courier courier);

    @Delete
    void deleteCourier(Courier courier);

    @Query("SELECT * FROM couriers WHERE courierId = :id")
    Courier getCourierById(int id);

    @Query("SELECT * FROM couriers")
    List<Courier> getAllCouriers();
}