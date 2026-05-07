package com.example.urisis_android.urinalysis

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One persisted urinalysis test, scoped to a single user.
 *
 * Tied to [com.example.urisis_android.auth.User] by email rather than
 * a Long FK because User uses email as its primary key.
 *
 * The denormalised classifier output ([urineClassName],
 * [statusName], [activeFlagsCsv]) lets the History screen render
 * without re-running the classifier — and keeps old records readable
 * even if the classifier is later retuned.
 */
@Entity(
    tableName = "test_records",
    indices = [Index("userEmail"), Index("timestampMillis")]
)
data class TestRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userEmail: String,
    val timestampMillis: Long,

    // Raw measured values
    val pH: Float,
    val tdsPpm: Float,
    val tempC: Float,
    val rgbR: Int,
    val rgbG: Int,
    val rgbB: Int,
    val rgbHex: String?,

    // Range checks
    val pHInRange: Boolean,
    val sgInRange: Boolean,
    val tdsInRange: Boolean,

    // Classifier output
    val urineClassName: String,           // UrineClass.name
    val statusName: String,               // HydrationStatus.name
    val confidence: Float,
    val activeFlagsCsv: String,           // "FLAG_A,FLAG_B" or empty
    val dominantMembership: Float,
)