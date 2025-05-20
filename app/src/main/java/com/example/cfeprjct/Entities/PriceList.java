// PriceList.java
package com.example.cfeprjct.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "price_list")
public class PriceList {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "priceListId")
    private int priceListId;

    @ColumnInfo(name = "drinkId")
    private Integer drinkId;

    @ColumnInfo(name = "dishId")
    private Integer dishId;

    @ColumnInfo(name = "dessertId")
    private Integer dessertId;

    @ColumnInfo(name = "price")
    private float price;

    @ColumnInfo(name = "date")
    private long date;

    // Геттеры/сеттеры

    public int getPriceListId() {
        return priceListId;
    }
    public void setPriceListId(int priceListId) {
        this.priceListId = priceListId;
    }

    public Integer getDrinkId() {
        return drinkId;
    }
    public void setDrinkId(Integer drinkId) {
        this.drinkId = drinkId;
    }

    public Integer getDishId() {
        return dishId;
    }
    public void setDishId(Integer dishId) {
        this.dishId = dishId;
    }

    public Integer getDessertId() {
        return dessertId;
    }
    public void setDessertId(Integer dessertId) {
        this.dessertId = dessertId;
    }

    public float getPrice() {
        return price;
    }
    public void setPrice(float price) {
        this.price = price;
    }

    public long getDate() {
        return date;
    }
    public void setDate(long date) {
        this.date = date;
    }
}
