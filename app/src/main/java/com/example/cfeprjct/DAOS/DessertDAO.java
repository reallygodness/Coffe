package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.example.cfeprjct.Entities.Dessert;

import java.util.List;

@Dao
public interface DessertDAO {

    @Insert
    long insertDessert(Dessert dessert);

    @Update
    void updateDessert(Dessert dessert);

    @Delete
    void deleteDessert(Dessert dessert);

    @Query("SELECT * FROM desserts WHERE dessertId = :dessertId LIMIT 1")
    Dessert getById(int dessertId);

    @Query("SELECT * FROM desserts")
    List<Dessert> getAllDesserts();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Dessert> desserts);

    @Query("SELECT * FROM desserts WHERE name LIKE :query")
    List<Dessert> searchDessertsByName(String query);
}