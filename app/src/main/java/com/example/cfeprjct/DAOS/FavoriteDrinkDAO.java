package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.FavoriteDrink;

import java.util.List;

@Dao
public interface FavoriteDrinkDAO {

    @Insert
    long insertFavoriteDrink(FavoriteDrink favoriteDrink);

    @Update
    void updateFavoriteDrink(FavoriteDrink favoriteDrink);

    @Delete
    void deleteFavoriteDrink(FavoriteDrink favoriteDrink);

    @Query("SELECT * FROM favorite_drinks WHERE favoriteDrinkId = :id")
    FavoriteDrink getFavoriteDrinkById(int id);

    @Query("SELECT * FROM favorite_drinks WHERE userId = :userId")
    List<FavoriteDrink> getFavoriteDrinksByUserId(String userId);
}