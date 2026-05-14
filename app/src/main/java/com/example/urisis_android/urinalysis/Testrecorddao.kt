package com.example.urisis_android.urinalysis

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestRecordDao {

    @Insert
    suspend fun insert(record: TestRecordEntity): Long

    /** All tests for a user, newest first. Used by the history list. */
    @Query("""
        SELECT * FROM test_records
        WHERE userEmail = :userEmail
        ORDER BY timestampMillis DESC
    """)
    fun observeForUser(userEmail: String): Flow<List<TestRecordEntity>>

    /** Tests within a time window, oldest first. Used for trend charts. */
    @Query("""
        SELECT * FROM test_records
        WHERE userEmail = :userEmail AND timestampMillis >= :sinceMillis
        ORDER BY timestampMillis ASC
    """)
    fun observeRangeForUser(
        userEmail: String,
        sinceMillis: Long
    ): Flow<List<TestRecordEntity>>

    @Query("DELETE FROM test_records WHERE userEmail = :userEmail")
    suspend fun clearForUser(userEmail: String)

    @Query("SELECT COUNT(*) FROM test_records WHERE userEmail = :userEmail")
    fun countForUser(userEmail: String): Flow<Int>

    @Query("SELECT * FROM test_records WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TestRecordEntity?
}