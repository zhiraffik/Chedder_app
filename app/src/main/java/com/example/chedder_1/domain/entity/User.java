package com.example.chedder_1.domain.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users",
        indices = {@Index("adminId")}
)

public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String email;

    public String password;
    public String phone;
    public String role; // admin / employee

    public String firstName;
    public String lastName;
    public String position;

    public double ratePerHour;
    public int hoursPerShift;

    public Integer adminId; // NULL для админа, != NULL для сотрудников
}

