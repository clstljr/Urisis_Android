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

    // Camera RGB (preferred classifier input — direct sample view)
    val cameraR: Int,
    val cameraG: Int,
    val cameraB: Int,
    val cameraHex: String?,

    // TCS34725 sensor RGB (reflected light off the strip).
    // Nullable so older records (or reads with no sensor) round-trip cleanly.
    val sensorR: Int?,
    val sensorG: Int?,
    val sensorB: Int?,
    val sensorHex: String?,

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
    /** Memberships for ALL classes, encoded as JSON: {"LEVEL_1":0.05,...}. */
    val membershipsJson: String? = null,
)