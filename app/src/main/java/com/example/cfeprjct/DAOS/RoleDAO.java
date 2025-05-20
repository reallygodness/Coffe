package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cfeprjct.Entities.Role;

import java.util.List;

@Dao
public interface RoleDAO {
    @Insert
    void insert(Role role);

    @Query("SELECT * FROM roles WHERE role_id = :id")
    Role getById(int id);

    @Query("SELECT * FROM roles")
    List<Role> getAll();
}
