package com.example.cfeprjct.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dishes")
public class Dish {
    @PrimaryKey(autoGenerate = true)
    private int dishId;

    private String name;        // Название
    private String description; // Описание

    @ColumnInfo(name = "imageUrl")
    private String imageUrl;

    private int size;

    // Геттеры и сеттеры
    public int getDishId() { return dishId; }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setDishId(int dishId) { this.dishId = dishId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
