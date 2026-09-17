package br.edu.ifpe.todo.notifications

/**
 * Cross-platform local-notification scheduler.
 * Implementations live in platform source sets:
 *  - Android: AlarmManager + NotificationManager
 *  - iOS: UNUserNotificationCenter
 */
expect class ReminderScheduler() {

    /**
     * Platform setup (e.g. create the notification channel on Android).
     * Safe to call more than once.
     */
    fun initialize()

    /**
     * Requests the OS notification permission, if required.
     * The callback receives [true] when notifications may be displayed.
     * Denial must never crash the app.
     */
    fun ensurePermission(onResult: (Boolean) -> Unit)

    /**
     * Schedules a local notification for [dueAtMillis] (epoch ms).
     * Calling again with the same [taskId] replaces the previous notification.
     */
    fun schedule(taskId: Long, taskTitle: String, dueAtMillis: Long)

    /**
     * Cancels any pending notification associated with [taskId].
     */
    fun cancel(taskId: Long)
}