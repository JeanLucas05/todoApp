package br.edu.ifpe.todo.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS implementation of local reminders built on UNUserNotificationCenter.
 * NOTE: this file compiles only on macOS with the Kotlin/Native toolchain.
 * It could not be compiled in the Linux development environment (see BUILD_LOG).
 */
@OptIn(ExperimentalForeignApi::class)
actual class ReminderScheduler {

    actual fun initialize() {
        // iOS has a single notification center; nothing to set up.
    }

    actual fun ensurePermission(onResult: (Boolean) -> Unit) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or
                UNAuthorizationOptionSound or
                UNAuthorizationOptionBadge
        ) { granted, _ ->
            onResult(granted)
        }
    }

    actual fun schedule(taskId: Long, taskTitle: String, dueAtMillis: Long) {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        val content = UNMutableNotificationContent().apply {
            title = "Lembrete de tarefa"
            body = "Tarefa: $taskTitle"
            sound = UNNotificationSound.defaultSound()
        }

        val date = NSDate.dateWithTimeIntervalSince1970(dueAtMillis / 1000.0)
        val units = NSCalendarUnitYear or
            NSCalendarUnitMonth or
            NSCalendarUnitDay or
            NSCalendarUnitHour or
            NSCalendarUnitMinute or
            NSCalendarUnitSecond
        val components = NSCalendar.currentCalendar.components(units, fromDate = date)
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            components,
            repeats = false,
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            notificationIdFor(taskId),
            content = content,
            trigger = trigger,
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    actual fun cancel(taskId: Long) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val id = notificationIdFor(taskId)
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(id))
    }

    private fun notificationIdFor(taskId: Long): String = "task_$taskId"
}