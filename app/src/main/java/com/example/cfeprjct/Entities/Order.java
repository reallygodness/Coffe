package com.example.cfeprjct.Entities;

import androidx.room.ColumnInfo;
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

    @ColumnInfo(name = "user_order_number")
    private int userOrderNumber;

    private String firestoreOrderId;

    private String courierId;        // ID курьера, который взял заказ
    private Long courierTakeTime;    // Время, когда курьер взял заказ (timestamp)
    private Long deliveryStartTime;  // Время начала доставки (timestamp)
    private Long deliveredTime;      // Время доставки (timestamp)

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


    public String getFirestoreOrderId() {
        return firestoreOrderId;
    }
    public void setFirestoreOrderId(String firestoreOrderId) {
        this.firestoreOrderId = firestoreOrderId;
    }


    public int getUserOrderNumber() { return userOrderNumber; }
    public void setUserOrderNumber(int n) { this.userOrderNumber = n; }

    public String getCourierId() {
        return courierId;
    }
    public void setCourierId(String courierId) {
        this.courierId = courierId;
    }

    public Long getCourierTakeTime() {
        return courierTakeTime;
    }
    public void setCourierTakeTime(Long courierTakeTime) {
        this.courierTakeTime = courierTakeTime;
    }

    public Long getDeliveryStartTime() {
        return deliveryStartTime;
    }
    public void setDeliveryStartTime(Long deliveryStartTime) {
        this.deliveryStartTime = deliveryStartTime;
    }

    public Long getDeliveredTime() {
        return deliveredTime;
    }
    public void setDeliveredTime(Long deliveredTime) {
        this.deliveredTime = deliveredTime;
    }
}
