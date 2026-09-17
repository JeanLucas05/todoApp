package br.edu.ifpe.todo.notifications

import br.edu.ifpe.todo.domain.Task
import kotlinx.datetime.Clock

/**
 * Business rules for reminder notifications.
 * Scheduling decisions are centralized here so every screen behaves the same way.
 */
class ReminderCoordinator(private val scheduler: ReminderScheduler) {

    /**
     * Called whenever a task is created, edited, completed or reopened.
     *
     * Rules:
     *  - completed task  -> cancel
     *  - no due date / due date already passed -> cancel
     *  - future due date -> (re)schedule
     */
    fun onTaskChanged(task: Task) {
        scheduler.initialize()
        val now = Clock.System.now().toEpochMilliseconds()
        val shouldSchedule = !task.completed && task.dueAtMillis != null && task.dueAtMillis > now
        if (shouldSchedule) {
            scheduler.ensurePermission { granted ->
                if (granted) scheduler.schedule(task.id, task.title, task.dueAtMillis!!)
            }
        } else {
            scheduler.cancel(task.id)
        }
    }

    /** Called when a task is deleted: its notification must be canceled. */
    fun onTaskDeleted(taskId: Long) {
        scheduler.initialize()
        scheduler.cancel(taskId)
    }
}