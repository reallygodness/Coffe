package com.example.cfeprjct.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "drinks")
public class Drink {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "drinkId")
    private int drinkId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    // Вот это поле не хватало
    @ColumnInfo(name = "volumeId")
    private int volumeId;

    // Геттеры
    public int getDrinkId() {
        return drinkId;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }

    // добавляем поле
    private String imageUrl;
    public int getVolumeId() {
        return volumeId;
    }

    // Сеттеры
    public void setDrinkId(int drinkId) {
        this.drinkId = drinkId;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public void setVolumeId(int volumeId) {
        this.volumeId = volumeId;
    }

    // новый геттер/сеттер для imageUrl
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
