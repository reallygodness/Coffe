// OrderedDrink.java
package com.example.cfeprjct.Entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "ordered_drinks",
        foreignKeys = @ForeignKey(
                entity = Order.class,
                parentColumns = "orderId",
                childColumns  = "orderId",
                onDelete      = CASCADE,
                onUpdate      = CASCADE
        )
)
public class OrderedDrink {
    @PrimaryKey(autoGenerate = true)
    private int orderedDrinkId;
    private int orderId;
    private int drinkId;
    private int quantity;

    private int size; // здесь храним выбранный объём в мл

    public int getOrderedDrinkId() { return orderedDrinkId; }
    public void setOrderedDrinkId(int id) { this.orderedDrinkId = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getDrinkId() { return drinkId; }
    public void setDrinkId(int drinkId) { this.drinkId = drinkId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
