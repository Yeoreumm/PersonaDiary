package com.example.personadiary;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DiaryDao {
    @Insert
    void insert(Diary diary);

    @Query("SELECT * FROM Diary ORDER BY id DESC")
    List<Diary> getAll();

    @Delete
    void delete(Diary diary);
}
