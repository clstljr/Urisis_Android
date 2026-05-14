package com.example.urisis_android.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Multi-account session storage.
 *
 * Replaces the old single-account [UserSession]. Stores a list of accounts
 * that have logged in on this device, plus a pointer to the currently
 * active one. Switching accounts is a one-tap pointer change; logout
 * removes an account from the device entirely.
 *
 * Schema in DataStore:
 *   - "accounts"        — JSON array of {email, name, addedAtMillis}
 *   - "active_email"    — email of the active account, or absent if logged out
 *
 * This file deliberately stores no passwords — credentials live in the
 * Room User table where they're salted/hashed. Switching accounts doesn't
 * re-prompt for password (matches Gmail/Twitter/Slack behaviour); the
 * device-level lock is the security boundary, and a future round will
 * add biometric/PIN gating for the whole app.
 */
private val Context.accountStore by preferencesDataStore(name = "accounts_store")

class AccountStore(private val context: Context) {

    private val ACCOUNTS_KEY = stringPreferencesKey("accounts")
    private val ACTIVE_EMAIL_KEY = stringPreferencesKey("active_email")

    data class StoredAccount(
        val email: String,
        val name: String,
        val addedAtMillis: Long,
    )

    /** All accounts logged in on this device, ordered by most-recently-added. */
    val accounts: Flow<List<StoredAccount>> = context.accountStore.data.map { prefs ->
        decodeAccounts(prefs[ACCOUNTS_KEY])
    }

    /** Email of the currently active account, or null if signed out. */
    val activeEmail: Flow<String?> = context.accountStore.data.map { prefs ->
        prefs[ACTIVE_EMAIL_KEY]?.takeIf { it.isNotBlank() }
    }

    /** The currently active account, or null if signed out. */
    val activeAccount: Flow<StoredAccount?> = context.accountStore.data.map { prefs ->
        val email = prefs[ACTIVE_EMAIL_KEY] ?: return@map null
        decodeAccounts(prefs[ACCOUNTS_KEY]).firstOrNull { it.email == email }
    }

    /**
     * Add an account (or update its name if it already exists) and mark
     * it as the active account. Called after a successful login or
     * registration.
     */
    suspend fun addAndActivate(email: String, name: String) {
        context.accountStore.edit { prefs ->
            val current = decodeAccounts(prefs[ACCOUNTS_KEY])
            val now = System.currentTimeMillis()
            val updated = current.filter { it.email != email } +
                    StoredAccount(email = email, name = name, addedAtMillis = now)
            // Sort newest first
            prefs[ACCOUNTS_KEY] = encodeAccounts(updated.sortedByDescending { it.addedAtMillis })
            prefs[ACTIVE_EMAIL_KEY] = email
        }
    }

    /**
     * Make an existing account the active one. Returns true on success,
     * false if the email isn't in the stored list.
     */
    suspend fun switchTo(email: String): Boolean {
        val list = context.accountStore.data.first().let {
            decodeAccounts(it[ACCOUNTS_KEY])
        }
        if (list.none { it.email == email }) return false
        context.accountStore.edit { prefs ->
            prefs[ACTIVE_EMAIL_KEY] = email
        }
        return true
    }

    /**
     * Remove an account from the device entirely. If it was the active
     * account, the active pointer is cleared (user will see the picker
     * or login screen on next launch).
     */
    suspend fun remove(email: String) {
        context.accountStore.edit { prefs ->
            val list = decodeAccounts(prefs[ACCOUNTS_KEY])
            val updated = list.filter { it.email != email }
            prefs[ACCOUNTS_KEY] = encodeAccounts(updated)
            if (prefs[ACTIVE_EMAIL_KEY] == email) {
                prefs.remove(ACTIVE_EMAIL_KEY)
            }
        }
    }

    /**
     * Sign out the active account but keep it in the stored list, so the
     * user can return without re-entering credentials.
     *
     * Use [remove] if the user wants to actually sign out *and* forget
     * the account on this device.
     */
    suspend fun deactivate() {
        context.accountStore.edit { prefs ->
            prefs.remove(ACTIVE_EMAIL_KEY)
        }
    }

    // ── (de)serialization ────────────────────────────────────────────────
    // Plain JSON in DataStore. Schema is small enough that adding Room or
    // a serialization library would be over-engineered.

    private fun encodeAccounts(list: List<StoredAccount>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("email", it.email)
                put("name", it.name)
                put("addedAt", it.addedAtMillis)
            })
        }
        return arr.toString()
    }

    private fun decodeAccounts(json: String?): List<StoredAccount> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                StoredAccount(
                    email = obj.optString("email").takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                    name = obj.optString("name", ""),
                    addedAtMillis = obj.optLong("addedAt", 0L),
                )
            }
        }.getOrElse { emptyList() }
    }
}