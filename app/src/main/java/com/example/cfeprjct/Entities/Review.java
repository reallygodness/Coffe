package com.example.cfeprjct.Entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reviews")
public class Review {
    @PrimaryKey(autoGenerate = true)
    private int reviewId;

    private String userId;    // id_пользователя
    private int drinkId;      // id_напитка
    private int dishId;       // id_блюда
    private int dessertId;    // id_десерта
    private int rating;       // Оценка
    private String text;      // Текст отзыва
    private long reviewDate;  // Дата отзыва (в мс)

    // Геттеры и сеттеры
    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getDrinkId() { return drinkId; }
    public void setDrinkId(int drinkId) { this.drinkId = drinkId; }
    public int getDishId() { return dishId; }
    public void setDishId(int dishId) { this.dishId = dishId; }
    public int getDessertId() { return dessertId; }
    public void setDessertId(int dessertId) { this.dessertId = dessertId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getReviewDate() { return reviewDate; }
    public void setReviewDate(long reviewDate) { this.reviewDate = reviewDate; }
}
