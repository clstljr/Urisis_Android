package com.example.urisis_android.urinalysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.urisis_android.auth.AppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * UI state for one day on the trend charts.
 */
data class DailyAggregate(
    val date: Date,
    val dayLabel: String,                  // "Feb 8"
    val avgHydrationPercent: Float,        // 0..100
    val avgPH: Float,
    val avgSpecificGravity: Float,
    val sampleCount: Int,
)

/**
 * ViewModel backing the History screen.
 *
 * Driven by the email of the currently logged-in user. Call
 * [setUser] when the auth user changes — usually once at construction
 * time from MainActivity.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo: HistoryRepository =
        HistoryRepository(AppDatabase.get(application).testRecordDao())

    private val userEmail = MutableStateFlow<String?>(null)

    /** All tests for the active user, newest first. Empty if no user set. */
    val tests: StateFlow<List<TestRecord>> =
        userEmail.flatMapLatest { email ->
            if (email.isNullOrBlank()) flowOf(emptyList())
            else repo.observeForUser(email)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Daily aggregates for the last 7 calendar days, oldest first.
     * Days with no tests are omitted — the chart only plots actual data
     * points.
     */
    val last7Days: StateFlow<List<DailyAggregate>> =
        userEmail.flatMapLatest { email ->
            if (email.isNullOrBlank()) flowOf(emptyList())
            else {
                val sinceMillis = System.currentTimeMillis() -
                        TimeUnit.DAYS.toMillis(SEVEN_DAYS_INCLUSIVE.toLong())
                repo.observeRangeForUser(email, sinceMillis)
                    .map { rows -> rows.toDailyAggregates() }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setUser(email: String?) {
        userEmail.value = email?.lowercase()?.trim()
    }

    fun clearHistory() {
        val email = userEmail.value ?: return
        viewModelScope.launch { repo.clearForUser(email) }
    }

    companion object {
        // 7 calendar days back, inclusive of today
        private const val SEVEN_DAYS_INCLUSIVE = 7
    }
}

@Suppress("UNCHECKED_CAST")
class HistoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HistoryViewModel(application) as T
}

// ── Aggregation helpers ───────────────────────────────────────────────────

private val MONTH_NAMES = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private fun List<TestRecord>.toDailyAggregates(): List<DailyAggregate> {
    if (isEmpty()) return emptyList()

    // Bucket records by calendar day (yyyy-DOY) using the device's
    // current timezone — sample dates are user-facing.
    val cal = Calendar.getInstance()
    val groups = LinkedHashMap<String, MutableList<TestRecord>>()
    for (r in this) {
        cal.time = r.timestamp
        val key = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        groups.getOrPut(key) { mutableListOf() }.add(r)
    }

    return groups.values.map { dayRecords ->
        val first = dayRecords.first().timestamp
        cal.time = first
        val month = MONTH_NAMES[cal.get(Calendar.MONTH)]
        val day = cal.get(Calendar.DAY_OF_MONTH)
        DailyAggregate(
            date = first,
            dayLabel = "$month $day",
            avgHydrationPercent =
                dayRecords.map { hydrationPercent(it.status).toFloat() }.average().toFloat(),
            avgPH = dayRecords.map { it.pH }.average().toFloat(),
            avgSpecificGravity =
                dayRecords.map { it.specificGravity }.average().toFloat(),
            sampleCount = dayRecords.size,
        )
    }.sortedBy { it.date }
}

/** Same numbers used by TestResultsScreen so the trend chart matches. */
internal fun hydrationPercent(s: HydrationStatus): Int = when (s) {
    HydrationStatus.OVERHYDRATED -> 95
    HydrationStatus.WELL_HYDRATED -> 80
    HydrationStatus.NORMAL -> 55
    HydrationStatus.MILDLY_DEHYDRATED -> 35
    HydrationStatus.DEHYDRATED -> 15
}