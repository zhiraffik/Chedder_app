package com.example.chedder_1.domain.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.chedder_1.domain.entity.WorkHours;

import java.util.List;

@Dao
public interface WorkHoursDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(WorkHours wh);

    @Query("SELECT * FROM work_hours WHERE adminId=:adminId AND userId=:userId AND year=:year AND month=:month")
    List<WorkHours> getMonth(int adminId, int userId, int year, int month);

    @Query("SELECT hours FROM work_hours WHERE adminId=:adminId AND userId=:userId AND year=:year AND month=:month AND day=:day LIMIT 1")
    Integer getHours(int adminId, int userId, int year, int month, int day);

    @Query("DELETE FROM work_hours WHERE adminId=:adminId AND userId=:userId AND year=:year AND month=:month AND day=:day")
    void deleteDay(int adminId, int userId, int year, int month, int day);

    @Query("DELETE FROM work_hours WHERE userId=:userId")
    void deleteAllForUser(int userId);
    @Query("SELECT startHour FROM work_hours WHERE adminId=:adminId AND userId=:userId AND year=:year AND month=:month AND day=:day LIMIT 1")
    Integer getStartHour(int adminId, int userId, int year, int month, int day);

    @Query("SELECT startMinute FROM work_hours WHERE adminId=:adminId AND userId=:userId AND year=:year AND month=:month AND day=:day LIMIT 1")
    Integer getStartMinute(int adminId, int userId, int year, int month, int day);

}
