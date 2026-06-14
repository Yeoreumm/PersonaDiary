package com.example.personadiary;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Diary {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String emotion;
    public String content;
    public String date;

    public Diary(String title, String emotion, String content, String date) {
        this.title = title;
        this.emotion = emotion;
        this.content = content;
        this.date = date;
    }
}
