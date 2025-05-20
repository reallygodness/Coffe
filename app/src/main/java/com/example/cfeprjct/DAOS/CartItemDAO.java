package com.example.cfeprjct.DAOS;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.CartItem;

import java.util.List;

@Dao
public interface CartItemDAO {
    @Query("SELECT * FROM cart_items")
    LiveData<List<CartItem>> getAll();

    @Query("SELECT * FROM cart_items WHERE productType=:type AND productId=:id LIMIT 1")
    CartItem getByProduct(String type, int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CartItem item);

    @Delete
    void delete(CartItem item);

    @Query("DELETE FROM cart_items")
    void clearAll();

    @Update
    void update(CartItem item);

    @Query("SELECT * FROM cart_items") List<CartItem> getAllSync();

    @Query("SELECT * FROM cart_items WHERE productType = :productType AND productId = :productId AND size = :size LIMIT 1")
    CartItem getByProductAndSize(String productType, int productId, int size);
    @Query("DELETE FROM cart_items")
    void clear();
}
