package com.example.urisis_android.auth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): User?

    /**
     * Login lookup: matches against either email or username so the
     * UI doesn't need to know which one the user typed.
     */
    @Query("SELECT * FROM users WHERE email = :id OR username = :id LIMIT 1")
    suspend fun findByEmailOrUsername(id: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}