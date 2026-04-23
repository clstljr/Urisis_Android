package com.example.urisis_android.auth

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,          // lowercased, used as login key
    val fullName: String,
    val passwordHash: String,               // never store plaintext
    val passwordSalt: String,               // per-user salt, base64
    val createdAt: Long = System.currentTimeMillis()
)