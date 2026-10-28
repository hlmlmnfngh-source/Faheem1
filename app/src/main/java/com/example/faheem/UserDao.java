package com.example.faheem;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;

@Dao
public interface UserDao {

    @Insert
    long insertUserAndGetId(User user);

    @Update
    void updateUser(User user);

    // نستخدمها وقت تسجيل الدخول (نتحقق هل الإيميل موجود)
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserById(int userId);

    @Query("DELETE FROM users WHERE id = :userId")
    void deleteUserById(int userId);
}
