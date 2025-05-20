package com.example.cfeprjct.Entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "couriers")
public class Courier {
    @PrimaryKey(autoGenerate = true)
    private int courierId;

    private String lastName;
    private String firstName;
    private String patronymic;
    private String phoneNumber;

    // Геттеры и сеттеры
    public int getCourierId() { return courierId; }
    public void setCourierId(int courierId) { this.courierId = courierId; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getPatronymic() { return patronymic; }
    public void setPatronymic(String patronymic) { this.patronymic = patronymic; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
