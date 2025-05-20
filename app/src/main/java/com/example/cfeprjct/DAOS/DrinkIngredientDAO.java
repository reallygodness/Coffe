package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.DrinkIngredient;

import java.util.List;

@Dao
public interface DrinkIngredientDAO {

    @Insert
    long insertDrinkIngredient(DrinkIngredient drinkIngredient);

    @Update
    void updateDrinkIngredient(DrinkIngredient drinkIngredient);

    @Delete
    void deleteDrinkIngredient(DrinkIngredient drinkIngredient);

    @Query("SELECT * FROM drinks_ingredients WHERE drinkIngredientId = :id")
    DrinkIngredient getDrinkIngredientById(int id);

    @Query("SELECT * FROM drinks_ingredients WHERE drinkId = :drinkId")
    List<DrinkIngredient> getIngredientsForDrink(int drinkId);
}
