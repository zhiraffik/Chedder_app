package com.example.chedder_1.domain.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.chedder_1.domain.entity.Schedule;

@Dao
public interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Schedule schedule);

    @Query("SELECT * FROM schedules " +
            "WHERE adminId = :adminId AND userId = :userId AND year = :year AND month = :month " +
            "LIMIT 1")
    Schedule getSchedule(int adminId, int userId, int year, int month);
}
