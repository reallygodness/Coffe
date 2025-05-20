// OrderedDrinkDAO.java
package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cfeprjct.Entities.OrderedDrink;

import java.util.List;

@Dao
public interface OrderedDrinkDAO {
    @Insert
    long insert(OrderedDrink od);

    @Query("SELECT * FROM ordered_drinks WHERE orderId = :orderId")
    List<OrderedDrink> getOrderedDrinksByOrderId(int orderId);

    /** Получить все напитки, привязанные к конкретному заказу */
    @Query("SELECT * FROM ordered_drinks WHERE orderId = :orderId")
    List<OrderedDrink> getByOrderId(int orderId);

    // вот этот метод нужно добавить:
    @Query("DELETE FROM ordered_drinks WHERE orderId = :orderId")
    void deleteByOrderId(int orderId);

    @Query("DELETE FROM ordered_drinks")
    void clearAll();
}
