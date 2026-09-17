package br.edu.ifpe.todo.ui.taskeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpe.todo.data.TodoRepository
import br.edu.ifpe.todo.domain.Category
import br.edu.ifpe.todo.domain.Task
import br.edu.ifpe.todo.notifications.ReminderCoordinator
import br.edu.ifpe.todo.util.TaskDueUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class TaskEditorUiState(
    val isNewTask: Boolean = true,
    val taskId: Long? = null,
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val canSave: Boolean = false,
)

class TaskEditorViewModel(
    private val taskId: Long?,
    private val repository: TodoRepository,
    private val reminders: ReminderCoordinator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskEditorUiState())
    val uiState: StateFlow<TaskEditorUiState> = _uiState

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val categories = repository.observeCategories().stateIn(
                this,
                SharingStarted.Eagerly,
                emptyList(),
            ).value

            val existing = taskId?.let { repository.fetchTask(it) }

            val (date, time) = existing?.dueAtMillis?.let { ms ->
                val ldt = Instant.fromEpochMilliseconds(ms)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                ldt.date to ldt.time
            } ?: (null to null)

            _uiState.value = TaskEditorUiState(
                isNewTask = existing == null,
                taskId = existing?.id,
                title = existing?.title.orEmpty(),
                description = existing?.description.orEmpty(),
                completed = existing?.completed == true,
                dueDate = date,
                dueTime = time,
                categoryId = existing?.categoryId,
                categories = categories,
                canSave = existing != null && existing.title.isNotBlank(),
            )
        }
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(
            title = value,
            canSave = value.isNotBlank(),
        )
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun onCompletedChange(completed: Boolean) {
        _uiState.value = _uiState.value.copy(completed = completed)
    }

    fun onCategoryChange(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
    }

    fun onDueDateChange(date: LocalDate?) {
        _uiState.value = _uiState.value.copy(dueDate = date)
    }

    fun onDueTimeChange(time: LocalTime?) {
        _uiState.value = _uiState.value.copy(dueTime = time)
    }

    fun clearDueDateTime() {
        _uiState.value = _uiState.value.copy(dueDate = null, dueTime = null)
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) return

        val dueMillis = TaskDueUtils.combineDueMillis(state.dueDate, state.dueTime)

        viewModelScope.launch {
            val savedTaskId: Long
            val task: Task
            if (state.isNewTask) {
                savedTaskId = repository.insertTask(
                    title = state.title.trim(),
                    description = state.description.trim().ifEmpty { null },
                    dueAtMillis = dueMillis,
                    categoryId = state.categoryId,
                    createdMillis = Clock.System.now().toEpochMilliseconds(),
                )
                task = repository.fetchTask(savedTaskId)!!
            } else {
                savedTaskId = state.taskId!!
                repository.updateTask(
                    taskId = savedTaskId,
                    title = state.title.trim(),
                    description = state.description.trim().ifEmpty { null },
                    completed = state.completed,
                    dueAtMillis = dueMillis,
                    categoryId = state.categoryId,
                )
                task = repository.fetchTask(savedTaskId)!!
            }
            reminders.onTaskChanged(task)
            onSaved()
        }
    }

    fun deleteTask(onDeleted: () -> Unit) {
        val id = _uiState.value.taskId ?: return
        viewModelScope.launch {
            repository.deleteTask(id)
            reminders.onTaskDeleted(id)
            onDeleted()
        }
    }

    }