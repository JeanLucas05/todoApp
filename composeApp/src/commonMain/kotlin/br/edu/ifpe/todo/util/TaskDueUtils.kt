package br.edu.ifpe.todo.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Helpers for handling optional due dates on a device.
 *
 * Due dates are persisted as epoch milliseconds. The M3 DatePicker works in
 * UTC-midnight milliseconds, so the conversions below go through TimeZone.UTC.
 */
object TaskDueUtils {

    val END_OF_DAY: LocalTime = LocalTime(hour = 23, minute = 59)

    /** @return true when a local notification should be scheduled for the task. */
    fun shouldScheduleReminder(dueAtMillis: Long?, completed: Boolean): Boolean {
        if (completed || dueAtMillis == null) return false
        return dueAtMillis > Clock.System.now().toEpochMilliseconds()
    }

    /** LocalDate -> UTC-midnight millis (as expected by M3 DatePicker). */
    fun toDatePickerMillis(date: LocalDate): Long =
        date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    /** UTC-midnight millis (from M3 DatePicker) -> LocalDate. */
    fun fromDatePickerMillis(millis: Long): LocalDate =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

    /**
     * Combines an optional date and an optional time into a single epoch millis
     * value in the device time zone.
     *
     * If only a date is provided, the time defaults to 23:59 of that day.
     * Returns null when no date is selected.
     */
    fun combineDueMillis(date: LocalDate?, time: LocalTime?): Long? {
        if (date == null) return null
        val effectiveTime = time ?: END_OF_DAY
        return LocalDateTime(date = date, time = effectiveTime)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }
}