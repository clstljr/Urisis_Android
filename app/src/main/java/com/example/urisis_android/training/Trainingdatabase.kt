package com.example.urisis_android.training

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.content.Context
import com.example.urisis_android.urinalysis.UrineClass

/**
 * Single labeled sensor measurement collected during a training session.
 *
 * Stored locally on the lab device only — this table never ships in the
 * production APK (an empty database is created on first launch, but nothing
 * ever writes to it outside Training Mode).
 */
@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Which controlled urine recipe this trial came from, e.g. "FORM-03". */
    @ColumnInfo(name = "formulation_id") val formulationId: String,

    /** Ground-truth class label for this formulation. */
    @ColumnInfo(name = "label") val label: UrineClass,

    /** Trial sequence number within this formulation (1-indexed). */
    @ColumnInfo(name = "trial_index") val trialIndex: Int,

    // Raw sensor values — pre-normalization.
    @ColumnInfo(name = "ph") val pH: Float,
    @ColumnInfo(name = "sg") val sg: Float,
    @ColumnInfo(name = "hue") val hue: Float,
    @ColumnInfo(name = "saturation") val saturation: Float,
    @ColumnInfo(name = "value") val value: Float,

    /** Epoch millis when the measurement was captured. */
    @ColumnInfo(name = "captured_at") val capturedAt: Long,

    /** Ambient temperature in Celsius — for diagnosing drift later. */
    @ColumnInfo(name = "ambient_temp_c") val ambientTempC: Float? = null,
)

@Dao
interface MeasurementDao {

    @Insert
    suspend fun insert(measurement: MeasurementEntity): Long

    @Query("SELECT * FROM measurements ORDER BY captured_at ASC")
    suspend fun loadAll(): List<MeasurementEntity>

    @Query("""
        SELECT formulation_id, label, COUNT(*) as trial_count
        FROM measurements
        GROUP BY formulation_id, label
        ORDER BY formulation_id ASC
    """)
    suspend fun loadProgress(): List<FormulationProgress>

    @Query("SELECT COUNT(*) FROM measurements")
    suspend fun totalCount(): Int

    @Query("DELETE FROM measurements WHERE formulation_id = :formulationId")
    suspend fun deleteFormulation(formulationId: String)

    @Query("DELETE FROM measurements")
    suspend fun deleteAll()
}

/** Lightweight projection used by the training dashboard's progress UI. */
data class FormulationProgress(
    @ColumnInfo(name = "formulation_id") val formulationId: String,
    @ColumnInfo(name = "label") val label: UrineClass,
    @ColumnInfo(name = "trial_count") val trialCount: Int,
)

class UrineClassConverter {
    @TypeConverter
    fun toString(cls: UrineClass): String = cls.name

    @TypeConverter
    fun fromString(value: String): UrineClass = UrineClass.valueOf(value)
}

@Database(entities = [MeasurementEntity::class], version = 1, exportSchema = false)
@TypeConverters(UrineClassConverter::class)
abstract class TrainingDatabase : RoomDatabase() {

    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile private var instance: TrainingDatabase? = null

        fun get(context: Context): TrainingDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrainingDatabase::class.java,
                    "urino_training.db",
                ).build().also { instance = it }
            }
        }
    }
}