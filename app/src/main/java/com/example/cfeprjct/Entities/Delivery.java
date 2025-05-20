package com.example.cfeprjct.Entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "deliveries")
public class Delivery {
        @PrimaryKey(autoGenerate = true)
        private int deliveryId;

        private int orderId;       // id_заказа
        private String courierId;  // id_курьера (может быть строкой, например, если используете UUID)
        private long deliveryDate; // Дата доставки (например, в миллисекундах)
        private String deliveryTime; // Время доставки (можно хранить как текст)

        // Геттеры и сеттеры
        public int getDeliveryId() { return deliveryId; }
        public void setDeliveryId(int deliveryId) { this.deliveryId = deliveryId; }
        public int getOrderId() { return orderId; }
        public void setOrderId(int orderId) { this.orderId = orderId; }
        public String getCourierId() { return courierId; }
        public void setCourierId(String courierId) { this.courierId = courierId; }
        public long getDeliveryDate() { return deliveryDate; }
        public void setDeliveryDate(long deliveryDate) { this.deliveryDate = deliveryDate; }
        public String getDeliveryTime() { return deliveryTime; }
        public void setDeliveryTime(String deliveryTime) { this.deliveryTime = deliveryTime; }
}
