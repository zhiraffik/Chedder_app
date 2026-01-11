package com.example.chedder_1.domain.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.chedder_1.domain.dao.ScheduleDao;
import com.example.chedder_1.domain.dao.UserDao;
import com.example.chedder_1.domain.dao.WorkHoursDao;
import com.example.chedder_1.domain.entity.Schedule;
import com.example.chedder_1.domain.entity.User;
import com.example.chedder_1.domain.entity.WorkHours;

@Database(
        entities = {User.class, Schedule.class, WorkHours.class},
        version = 8
)
@TypeConverters(Converters.class)
public abstract class AppDataBase extends RoomDatabase {

    private static AppDataBase INSTANCE;

    public abstract UserDao userDao();
    public abstract WorkHoursDao workHoursDao();
    public abstract ScheduleDao scheduleDao();

    public static synchronized AppDataBase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDataBase.class,
                            "app_db"
                    )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}
