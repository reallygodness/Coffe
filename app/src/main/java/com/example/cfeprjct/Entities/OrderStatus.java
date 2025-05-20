package com.example.cfeprjct.Entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "order_statuses")
public class OrderStatus {
    @PrimaryKey
    private int statusId;

    private String statusName;

    public int getStatusId() { return statusId; }
    public void setStatusId(int id) { this.statusId = id; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String name) { this.statusName = name; }
}
