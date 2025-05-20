package com.example.cfeprjct.Entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders",
        foreignKeys = @ForeignKey(
                entity = OrderStatus.class,
                parentColumns = "statusId",
                childColumns  = "statusId",
                onUpdate      = ForeignKey.CASCADE,
                onDelete      = ForeignKey.RESTRICT
        )
)
public class Order {
    @PrimaryKey(autoGenerate = true)
    private int orderId;

    private String userId;
    private float totalPrice;

    // теперь — не String status, а ссылка на OrderStatus.statusId
    private int statusId;

    private long createdAt;

    // геттеры/сеттеры...

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public float getTotalPrice() { return totalPrice; }
    public void setTotalPrice(float totalPrice) { this.totalPrice = totalPrice; }

    public int getStatusId() { return statusId; }
    public void setStatusId(int statusId) { this.statusId = statusId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
