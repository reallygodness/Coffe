// OrderedDish.java
package com.example.cfeprjct.Entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "ordered_dishes",
        foreignKeys = @ForeignKey(
                entity = Order.class,
                parentColumns = "orderId",
                childColumns  = "orderId",
                onDelete      = CASCADE,
                onUpdate      = CASCADE
        )
)
public class OrderedDish {
    @PrimaryKey(autoGenerate = true)
    private int orderedDishId;
    private int orderId;
    private int dishId;
    private int quantity;

    private int size; // вес в граммах

    public int getOrderedDishId() { return orderedDishId; }
    public void setOrderedDishId(int id) { this.orderedDishId = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getDishId() { return dishId; }
    public void setDishId(int dishId) { this.dishId = dishId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
