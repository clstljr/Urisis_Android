package com.example.urisis_android.urinalysis

import com.example.urisis_android.bluetooth.RgbColor
import com.example.urisis_android.bluetooth.SensorReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.Date

class HistoryRepository(private val dao: TestRecordDao) {

    fun observeForUser(userEmail: String): Flow<List<TestRecord>> =
        dao.observeForUser(userEmail).map { rows -> rows.map { it.toDomain() } }

    fun observeRangeForUser(userEmail: String, sinceMillis: Long): Flow<List<TestRecord>> =
        dao.observeRangeForUser(userEmail, sinceMillis)
            .map { rows -> rows.map { it.toDomain() } }

    fun countForUser(userEmail: String): Flow<Int> = dao.countForUser(userEmail)

    suspend fun findById(id: Long): TestRecord? = dao.findById(id)?.toDomain()

    suspend fun save(userEmail: String, result: UrinalysisResult): Long {
        val cam = result.reading.cameraRgb
        val sen = result.reading.sensorRgb
        // Camera columns are non-null in the schema. If there's no camera
        // (older device, future config), fall back to the sensor RGB so
        // we still have a swatch to display.
        val effectiveCam = cam ?: sen ?: RgbColor(0, 0, 0)
        return dao.insert(
            TestRecordEntity(
                userEmail = userEmail,
                timestampMillis = result.timestamp.time,
                pH = result.reading.pH ?: Float.NaN,
                tdsPpm = result.reading.tdsPpm ?: Float.NaN,
                tempC = result.reading.tempC ?: Float.NaN,
                cameraR = effectiveCam.r,
                cameraG = effectiveCam.g,
                cameraB = effectiveCam.b,
                cameraHex = effectiveCam.hex,
                sensorR = sen?.r,
                sensorG = sen?.g,
                sensorB = sen?.b,
                sensorHex = sen?.hex,
                pHInRange = result.pHInRange,
                sgInRange = result.sgInRange,
                tdsInRange = result.tdsInRange,
                urineClassName = result.urineClass.name,
                statusName = result.status.name,
                confidence = result.confidence,
                activeFlagsCsv = result.activeFlags.joinToString(",") { it.name },
                dominantMembership = result.memberships[result.urineClass] ?: 0f,
                membershipsJson = encodeMemberships(result.memberships),
            )
        )
    }

    suspend fun clearForUser(userEmail: String) = dao.clearForUser(userEmail)
}

/**
 * Lightweight domain representation of a stored test. Carries enough
 * information to reconstruct a full [UrinalysisResult] via
 * [toUrinalysisResult].
 */
data class TestRecord(
    val id: Long,
    val timestamp: Date,
    val pH: Float,
    val tdsPpm: Float,
    val tempC: Float,
    val specificGravity: Float,
    val cameraRgb: RgbColor,
    val sensorRgb: RgbColor?,
    val pHInRange: Boolean,
    val sgInRange: Boolean,
    val tdsInRange: Boolean,
    val urineClass: UrineClass,
    val status: HydrationStatus,
    val confidence: Float,
    val activeFlags: List<UrineClass>,
    val dominantMembership: Float,
    val memberships: Map<UrineClass, Float>,
) {
    /**
     * Backward-compat alias — returns whichever RGB the classifier used
     * (camera in the new schema).
     */
    val rgb: RgbColor get() = cameraRgb
}

/**
 * Reconstruct a full [UrinalysisResult] from a stored record so the
 * existing TestResultsScreen / AdvancedResultsScreen composables can
 * render historical tests without modification.
 */
fun TestRecord.toUrinalysisResult(): UrinalysisResult {
    val reading = SensorReading(
        pH = pH,
        tdsPpm = tdsPpm,
        tempC = tempC,
        sensorRgb = sensorRgb,
        cameraRgb = cameraRgb,
    )
    // Recompute colorLabel from the reconstructed HSV so the main
    // result screen's color card has a non-empty label when displaying
    // a stored test. Cheaper than persisting the label separately.
    val recomputedLabel = reading.color
        ?.let { colorLabelFor(it, activeFlags) }
        ?: ""
    return UrinalysisResult(
        reading = reading,
        status = status,
        confidence = confidence,
        pHInRange = pHInRange,
        sgInRange = sgInRange,
        tdsInRange = tdsInRange,
        colorLabel = recomputedLabel,
        urineClass = urineClass,
        activeFlags = activeFlags,
        memberships = memberships,
        timestamp = timestamp,
    )
}

internal fun TestRecordEntity.toDomain(): TestRecord {
    val urineCls = runCatching { UrineClass.valueOf(urineClassName) }
        .getOrDefault(UrineClass.LEVEL_2)
    val status   = runCatching { HydrationStatus.valueOf(statusName) }
        .getOrDefault(HydrationStatus.NORMAL)
    val flags = if (activeFlagsCsv.isBlank()) emptyList()
    else activeFlagsCsv.split(",").mapNotNull {
        runCatching { UrineClass.valueOf(it) }.getOrNull()
    }
    val sensor = if (sensorR != null && sensorG != null && sensorB != null) {
        RgbColor(sensorR, sensorG, sensorB, sensorHex)
    } else null
    // Decode full memberships if present. Fallback: synthesize a minimal
    // map from the dominant class + flags so the result screen has
    // non-zero bars to show.
    val membershipMap = decodeMemberships(membershipsJson).ifEmpty {
        buildMap {
            put(urineCls, dominantMembership)
            flags.forEach { put(it, ClassificationResult.FLAG_THRESHOLD + 0.05f) }
        }
    }
    return TestRecord(
        id = id,
        timestamp = Date(timestampMillis),
        pH = pH,
        tdsPpm = tdsPpm,
        tempC = tempC,
        specificGravity = 1.000f + (tdsPpm.coerceAtLeast(0f) / SensorReading.SG_DIVISOR),
        cameraRgb = RgbColor(cameraR, cameraG, cameraB, cameraHex),
        sensorRgb = sensor,
        pHInRange = pHInRange,
        sgInRange = sgInRange,
        tdsInRange = tdsInRange,
        urineClass = urineCls,
        status = status,
        confidence = confidence,
        activeFlags = flags,
        dominantMembership = dominantMembership,
        memberships = membershipMap,
    )
}

private fun encodeMemberships(m: Map<UrineClass, Float>): String {
    val obj = JSONObject()
    m.forEach { (k, v) -> obj.put(k.name, v.toDouble()) }
    return obj.toString()
}

private fun decodeMemberships(json: String?): Map<UrineClass, Float> {
    if (json.isNullOrBlank()) return emptyMap()
    return runCatching {
        val obj = JSONObject(json)
        buildMap {
            obj.keys().forEach { key ->
                val cls = runCatching { UrineClass.valueOf(key) }.getOrNull()
                if (cls != null) put(cls, obj.optDouble(key, 0.0).toFloat())
            }
        }
    }.getOrElse { emptyMap() }
}