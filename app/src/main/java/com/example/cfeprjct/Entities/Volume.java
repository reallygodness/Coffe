package com.example.cfeprjct.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "volumes")
public class Volume {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "volumeId")
    private int volumeId;

    @ColumnInfo(name = "size")
    private String size;

    @ColumnInfo(name = "ml")
    private int ml;

    // Геттеры
    public int getVolumeId() { return volumeId; }
    public String getSize()  { return size;     }
    public int getMl()       { return ml;       }

    // Сеттеры
    public void setVolumeId(int volumeId) { this.volumeId = volumeId; }
    public void setSize(String size)      { this.size = size;         }
    public void setMl(int ml)             { this.ml = ml;             }
}
