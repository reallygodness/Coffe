// OrderedDessert.java
package com.example.cfeprjct.Entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "ordered_desserts",
        foreignKeys = @ForeignKey(
                entity = Order.class,
                parentColumns = "orderId",
                childColumns  = "orderId",
                onDelete      = CASCADE,
                onUpdate      = CASCADE
        )
)
public class OrderedDessert {
    @PrimaryKey(autoGenerate = true)
    private int orderedDessertId;
    private int orderId;
    private int dessertId;
    private int quantity;

    private int size; // вес в граммах

    public int getOrderedDessertId() { return orderedDessertId; }
    public void setOrderedDessertId(int id) { this.orderedDessertId = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getDessertId() { return dessertId; }
    public void setDessertId(int dessertId) { this.dessertId = dessertId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
