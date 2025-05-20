package com.example.cfeprjct.Entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "drinks_ingredients")
public class DrinkIngredient {
    @PrimaryKey(autoGenerate = true)
    private int drinkIngredientId;

    private int drinkId;      // id_Напитка
    private int ingredientId; // id_Ингредиента
    private float quantity;   // Количество ингредиента

    // Геттеры и сеттеры
    public int getDrinkIngredientId() { return drinkIngredientId; }
    public void setDrinkIngredientId(int drinkIngredientId) { this.drinkIngredientId = drinkIngredientId; }
    public int getDrinkId() { return drinkId; }
    public void setDrinkId(int drinkId) { this.drinkId = drinkId; }
    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }
    public float getQuantity() { return quantity; }
    public void setQuantity(float quantity) { this.quantity = quantity; }
}
