package com.example.cfeprjct.Entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_drinks")
public class FavoriteDrink {
    @PrimaryKey(autoGenerate = true)
    private int favoriteDrinkId;

    private String userId; // id_Пользователя
    private int drinkId;   // id_Напитка

    // Геттеры и сеттеры
    public int getFavoriteDrinkId() { return favoriteDrinkId; }
    public void setFavoriteDrinkId(int favoriteDrinkId) { this.favoriteDrinkId = favoriteDrinkId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getDrinkId() { return drinkId; }
    public void setDrinkId(int drinkId) { this.drinkId = drinkId; }
}
