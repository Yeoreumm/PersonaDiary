package com.example.personadiary;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Diary.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DiaryDao diaryDao();
}
