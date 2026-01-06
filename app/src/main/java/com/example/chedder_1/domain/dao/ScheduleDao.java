package com.example.chedder_1.domain.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chedder_1.domain.entity.Schedule;


@Dao
public interface ScheduleDao {

    @Insert
    void insert(Schedule schedule);

    @Query("SELECT * FROM schedules " +
            "WHERE userId = :userId AND year = :year AND month = :month " +
            "LIMIT 1")
    Schedule getSchedule(int userId, int year, int month);


    @Update
    void update(Schedule schedule);
}
