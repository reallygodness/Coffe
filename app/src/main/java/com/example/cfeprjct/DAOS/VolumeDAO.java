package com.example.cfeprjct.DAOS;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cfeprjct.Entities.Volume;

import java.util.List;

@Dao
public interface VolumeDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Volume> volumes);

    @Query("SELECT * FROM volumes")
    List<Volume> getAllVolumes();

    // при необходимости — поиск по id
    @Query("SELECT * FROM volumes WHERE volumeId = :id")
    Volume getById(int id);

    @Query("SELECT * FROM volumes WHERE size = :sizeLabel LIMIT 1")
    Volume getBySize(String sizeLabel);
}
