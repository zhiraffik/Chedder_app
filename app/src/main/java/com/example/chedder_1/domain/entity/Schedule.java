package com.example.chedder_1.domain.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(
        tableName = "schedules",
        foreignKeys = {
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "id",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "id",
                        childColumns = "adminId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("userId"),
                @Index("adminId"),
                @Index(value = {"adminId", "userId", "year", "month"}, unique = true)
        }
)
public class Schedule {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public int adminId;

    public int year;
    public int month;

    public List<Integer> workDays = new ArrayList<>();
    public List<Integer> vacationDays = new ArrayList<>();
    public List<Integer> cleaningDays = new ArrayList<>();
    public List<Integer> canteenDutyDays = new ArrayList<>();
    public List<Integer> lunchPrepDutyDays = new ArrayList<>();
}
