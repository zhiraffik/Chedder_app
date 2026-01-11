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
    long insert(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    User login(String email, String password);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getById(int id);

    @Query("SELECT * FROM users WHERE role = 'employee' AND adminId = :adminId ORDER BY lastName, firstName")
    List<User> getEmployeesForAdmin(int adminId);

    @Query("SELECT * FROM users WHERE role = 'admin' LIMIT 1")
    User getAnyAdmin();

    @Query("SELECT * FROM users")
    List<User> getAll();

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getByEmail(String email);

    // ✅ нужно для проверки уникальности email при редактировании
    @Query("SELECT * FROM users WHERE email = :email AND id != :excludeId LIMIT 1")
    User getByEmailExceptId(String email, int excludeId);

    // ✅ нужно для удаления сотрудника
    @Query("DELETE FROM users WHERE id = :id")
    void deleteById(int id);
}
