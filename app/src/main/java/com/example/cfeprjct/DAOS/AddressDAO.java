package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cfeprjct.Entities.Address;

import java.util.List;

@Dao
public interface AddressDAO {


    @Delete
    void deleteAddress(Address address);

    @Query("SELECT * FROM addresses WHERE userId = :userId")
    List<Address> getAddressesByUserId(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Address address);

    @Query("SELECT * FROM addresses WHERE userId = :userId LIMIT 1")
    Address getByUserId(String userId);

    /** Вставка или обновление; возвращаем rowId при вставке */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertAddress(Address address);

    /** Обновление существующей записи */
    @Update
    void updateAddress(Address address);

    /** Поиск по userId */
    @Query("SELECT * FROM addresses WHERE userId = :userId")
    Address getAddressByUserId(String userId);

}
