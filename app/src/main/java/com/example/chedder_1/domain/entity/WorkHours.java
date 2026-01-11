package com.example.chedder_1.domain.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "work_hours",
        indices = {
                @Index(value = {"adminId","userId","year","month","day"}, unique = true),
                @Index("adminId"),
                @Index("userId")
        }
)
public class WorkHours {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int adminId;
    public int userId;
    public int startHour;
    public int startMinute;

    public int year;
    public int month;
    public int day;

    public int hours;
}
