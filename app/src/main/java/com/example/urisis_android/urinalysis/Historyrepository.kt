package com.example.urisis_android.urinalysis

import com.example.urisis_android.bluetooth.RgbColor
import com.example.urisis_android.bluetooth.SensorReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

/**
 * Repository for persisted urinalysis history.
 *
 * Translates between [UrinalysisResult] (in-memory domain) and
 * [TestRecordEntity] (Room storage). Callers see plain [TestRecord]
 * objects with mapped enums.
 */
class HistoryRepository(private val dao: TestRecordDao) {

    fun observeForUser(userEmail: String): Flow<List<TestRecord>> =
        dao.observeForUser(userEmail).map { rows -> rows.map { it.toDomain() } }

    fun observeRangeForUser(userEmail: String, sinceMillis: Long): Flow<List<TestRecord>> =
        dao.observeRangeForUser(userEmail, sinceMillis)
            .map { rows -> rows.map { it.toDomain() } }

    fun countForUser(userEmail: String): Flow<Int> = dao.countForUser(userEmail)

    suspend fun save(userEmail: String, result: UrinalysisResult): Long {
        val rgb = result.reading.rgb
        return dao.insert(
            TestRecordEntity(
                userEmail = userEmail,
                timestampMillis = result.timestamp.time,
                pH = result.reading.pH ?: Float.NaN,
                tdsPpm = result.reading.tdsPpm ?: Float.NaN,
                tempC = result.reading.tempC ?: Float.NaN,
                rgbR = rgb?.r ?: 0,
                rgbG = rgb?.g ?: 0,
                rgbB = rgb?.b ?: 0,
                rgbHex = rgb?.hex,
                pHInRange = result.pHInRange,
                sgInRange = result.sgInRange,
                tdsInRange = result.tdsInRange,
                urineClassName = result.urineClass.name,
                statusName = result.status.name,
                confidence = result.confidence,
                activeFlagsCsv = result.activeFlags.joinToString(",") { it.name },
                dominantMembership = result.memberships[result.urineClass] ?: 0f,
            )
        )
    }

    suspend fun clearForUser(userEmail: String) = dao.clearForUser(userEmail)
}

/**
 * Lightweight domain representation of a stored test, minus the heavy
 * [UrinalysisResult] machinery (which is rebuildable from these fields
 * if needed).
 */
data class TestRecord(
    val id: Long,
    val timestamp: Date,
    val pH: Float,
    val tdsPpm: Float,
    val specificGravity: Float,
    val rgb: RgbColor,
    val pHInRange: Boolean,
    val sgInRange: Boolean,
    val tdsInRange: Boolean,
    val urineClass: UrineClass,
    val status: HydrationStatus,
    val confidence: Float,
    val activeFlags: List<UrineClass>,
    val dominantMembership: Float,
)

private fun TestRecordEntity.toDomain(): TestRecord {
    val urineCls = runCatching { UrineClass.valueOf(urineClassName) }
        .getOrDefault(UrineClass.LEVEL_2)
    val status   = runCatching { HydrationStatus.valueOf(statusName) }
        .getOrDefault(HydrationStatus.NORMAL)
    val flags = if (activeFlagsCsv.isBlank()) emptyList()
    else activeFlagsCsv.split(",").mapNotNull {
        runCatching { UrineClass.valueOf(it) }.getOrNull()
    }
    return TestRecord(
        id = id,
        timestamp = Date(timestampMillis),
        pH = pH,
        tdsPpm = tdsPpm,
        // Derived SG using the same TDS→SG mapping the classifier uses
        specificGravity = 1.000f + (tdsPpm.coerceAtLeast(0f) / SensorReading.SG_DIVISOR),
        rgb = RgbColor(rgbR, rgbG, rgbB, rgbHex),
        pHInRange = pHInRange,
        sgInRange = sgInRange,
        tdsInRange = tdsInRange,
        urineClass = urineCls,
        status = status,
        confidence = confidence,
        activeFlags = flags,
        dominantMembership = dominantMembership,
    )
}