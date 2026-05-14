package com.example.urisis_android.auth

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local account record.
 *
 * - [email] remains the primary key for back-compat with TestRecordEntity,
 *   which references users by email.
 * - [username] is unique and lower-cased; users may log in with either
 *   email or username.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey val email: String,
    val username: String,
    val fullName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAt: Long = System.currentTimeMillis()
)