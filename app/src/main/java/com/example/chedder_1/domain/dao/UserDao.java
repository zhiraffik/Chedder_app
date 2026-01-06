package com.example.chedder_1.domain.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.chedder_1.domain.entity.User;

import java.util.List;

@Dao
public interface UserDao {

    @Insert
    void insert(User user);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    User login(String email, String password);

    @Query("SELECT * FROM users WHERE id = :id")
    User getById(int id);

    @Query("SELECT * FROM users")
    List<User> getAll();

    @Update
    void update(User user);
}
