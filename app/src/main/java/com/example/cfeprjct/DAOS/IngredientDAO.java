package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.Ingredient;

import java.util.List;

@Dao
public interface IngredientDAO {

    @Insert
    long insertIngredient(Ingredient ingredient);

    @Update
    void updateIngredient(Ingredient ingredient);

    @Delete
    void deleteIngredient(Ingredient ingredient);

    @Query("SELECT * FROM ingredients WHERE ingredientId = :id")
    Ingredient getIngredientById(int id);

    @Query("SELECT * FROM ingredients")
    List<Ingredient> getAllIngredients();
}