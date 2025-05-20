// OrderedDishDAO.java
package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cfeprjct.Entities.OrderedDish;

import java.util.List;

@Dao
public interface OrderedDishDAO {
    @Insert
    long insert(OrderedDish od);

    @Query("SELECT * FROM ordered_dishes WHERE orderId = :orderId")
    List<OrderedDish> getOrderedDishesByOrderId(int orderId);

    @Query("SELECT * FROM ordered_dishes WHERE orderId = :orderId")
    List<OrderedDish> getByOrderId(int orderId);

    @Query("DELETE FROM ordered_dishes WHERE orderId = :orderId")
    void deleteByOrderId(int orderId);

    @Query("DELETE FROM ordered_dishes")
    void clearAll();
}
