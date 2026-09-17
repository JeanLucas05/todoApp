package br.edu.ifpe.todo.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.edu.ifpe.todo.AndroidAppContext

/**
 * Android implementation of local reminders.
 *
 * Strategy:
 *  - one [AlarmManager] alarm per task, keyed by [taskId] as the request code;
 *  - the alarm broadcasts an explicit intent to [ReminderReceiver], which posts
 *    the local notification;
 *  - permission: on API 33+ we request POST_NOTIFICATIONS before scheduling.
 *    If denied, scheduling is still attempted but the notification is simply
 *    not displayed (the app never crashes);
 *  - on Android 14+ SCHEDULE_EXACT_ALARM may be revoked: we catch
 *    SecurityException and fall back to an inexact alarm.
 */
actual class ReminderScheduler {

    actual fun initialize() {
        val ctx = AndroidAppContext.context
        createReminderChannel(ctx)
    }

    actual fun ensurePermission(onResult: (Boolean) -> Unit) {
        val context = AndroidAppContext.context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) {
                onResult(true)
                return
            }
            val activity = AndroidAppContext.activity
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS,
                )
                // Result is answered asynchronously; assume granted to avoid
                // blocking the flow. If the user denies it, the notification
                // just won't be displayed.
                onResult(true)
            } else {
                onResult(false)
            }
        } else {
            onResult(true)
        }
    }

    actual fun schedule(taskId: Long, taskTitle: String, dueAtMillis: Long) {
        val context = AndroidAppContext.context
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = newPendingIntent(context, taskId, taskTitle)

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dueAtMillis,
                pendingIntent,
            )
        } catch (security: SecurityException) {
            // SCHEDULE_EXACT_ALARM revoked: use an inexact-but-close alarm.
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                dueAtMillis,
                60_000L,
                pendingIntent,
            )
        }
    }

    actual fun cancel(taskId: Long) {
        val context = AndroidAppContext.context
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        findPendingIntent(context, taskId)?.let { pendingIntent ->
            alarmManager.cancel(pendingIntent)
        }
    }

    /** Creates (or replaces) the alarm PendingIntent for a task. Never null. */
    private fun newPendingIntent(context: Context, taskId: Long, taskTitle: String): PendingIntent {
        val intent = alarmIntent(context, taskId, taskTitle)
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Fetches an existing alarm PendingIntent without creating a new one. */
    private fun findPendingIntent(context: Context, taskId: Long): PendingIntent? {
        val intent = alarmIntent(context, taskId, "")
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(taskId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmIntent(context: Context, taskId: Long, taskTitle: String): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_TITLE, taskTitle)
        }

    private fun requestCodeFor(taskId: Long): Int = taskId.toInt()

    private companion object {
        const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }
}