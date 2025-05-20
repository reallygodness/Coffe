// PriceListDAO.java
package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.PriceList;

import java.util.List;

@Dao
public interface PriceListDAO {

    @Insert
    long insertPriceList(PriceList priceList);

    @Update
    void updatePriceList(PriceList priceList);

    @Delete
    void deletePriceList(PriceList priceList);

    @Query("SELECT * FROM price_list WHERE priceListId = :id")
    PriceList getPriceListById(int id);

    @Query("SELECT * FROM price_list")
    List<PriceList> getAllPriceLists();

    @Query("SELECT price FROM price_list WHERE drinkId = :drinkId ORDER BY date DESC LIMIT 1")
    float getLatestPriceForDrink(int drinkId);

    @Query("SELECT price FROM price_list WHERE dishId = :dishId ORDER BY date DESC LIMIT 1")
    float getLatestPriceForDish(int dishId);

    @Query("SELECT price FROM price_list WHERE dessertId = :dessertId ORDER BY date DESC LIMIT 1")
    float getLatestPriceForDessert(int dessertId);

    /** Вставляет или обновляет сразу несколько записей */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PriceList> prices);

}
