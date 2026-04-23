package com.example.urisis_android.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionStore by preferencesDataStore(name = "user_session")

class UserSession(private val context: Context) {

    private val EMAIL = stringPreferencesKey("email")
    private val NAME  = stringPreferencesKey("name")

    data class Session(val email: String, val name: String)

    val current: Flow<Session?> = context.sessionStore.data.map { prefs ->
        val email = prefs[EMAIL] ?: return@map null
        val name  = prefs[NAME] ?: return@map null
        Session(email, name)
    }

    suspend fun save(email: String, name: String) {
        context.sessionStore.edit { prefs ->
            prefs[EMAIL] = email
            prefs[NAME] = name
        }
    }

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }
}