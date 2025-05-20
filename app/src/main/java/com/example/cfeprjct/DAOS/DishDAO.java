package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.Dish;
import com.example.cfeprjct.Entities.Drink;

import java.util.List;

@Dao
public interface DishDAO {

    @Insert
    long insertDish(Dish dish);

    @Update
    void updateDish(Dish dish);

    @Delete
    void deleteDish(Dish dish);

    @Query("SELECT * FROM dishes WHERE dishId = :dishId LIMIT 1")
    Dish getById(int dishId);

    @Query("SELECT * FROM dishes WHERE dishId = :id")
    Dish getDishById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Dish> dishes);


    @Query("SELECT * FROM dishes WHERE name LIKE :query")
    List<Dish> searchDishesByName(String query);


    @Query("SELECT * FROM dishes")
    List<Dish> getAllDishes();
}