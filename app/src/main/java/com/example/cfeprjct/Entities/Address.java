package com.example.cfeprjct.Entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "addresses")
public class Address {
    @PrimaryKey(autoGenerate = true)
    private int addressId;

    private String userId; // id_пользователя
    private String city;   // Город
    private String street; // Улица
    private String house;  // Дом
    private String apartment; // Квартира

    // Геттеры и сеттеры
    public int getAddressId() { return addressId; }
    public void setAddressId(int addressId) { this.addressId = addressId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getHouse() { return house; }
    public void setHouse(String house) { this.house = house; }
    public String getApartment() { return apartment; }
    public void setApartment(String apartment) { this.apartment = apartment; }
}
