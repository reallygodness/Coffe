package com.example.cfeprjct;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber LIMIT 1")
    User getUserByPhoneNumber(String phoneNumber);  // Метод для проверки наличия пользователя по номеру телефона

    @Update
    void updateUser(User user);  // Этот метод будет использоваться для обновления данных пользователя

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);
    @Query("SELECT * FROM users")
    List<User> getAllUsers(); // Получить всех пользователей

    @Query("DELETE FROM users WHERE userId = :userId")
    void deleteUserById(String userId);

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    User getUserById(String userId);



}
