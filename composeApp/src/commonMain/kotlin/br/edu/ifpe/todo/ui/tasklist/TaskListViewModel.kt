package br.edu.ifpe.todo.ui.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpe.todo.data.TodoRepository
import br.edu.ifpe.todo.domain.Category
import br.edu.ifpe.todo.domain.Task
import br.edu.ifpe.todo.domain.TaskStatusFilter
import br.edu.ifpe.todo.notifications.ReminderCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val statusFilter: TaskStatusFilter = TaskStatusFilter.ALL,
    val categoryFilterId: Long? = null,
)

class TaskListViewModel(
    private val repository: TodoRepository,
    private val reminders: ReminderCoordinator,
) : ViewModel() {

    private val statusFilter = MutableStateFlow(TaskStatusFilter.ALL)
    private val categoryFilterId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<TaskListUiState> = combine(
        repository.observeTasks(),
        repository.observeCategories(),
        statusFilter,
        categoryFilterId,
    ) { tasks, categories, status, categoryId ->
        TaskListUiState(
            tasks = tasks,
            categories = categories,
            statusFilter = status,
            categoryFilterId = categoryId,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TaskListUiState(),
    )

    fun filteredTasks(state: TaskListUiState): List<Task> =
        state.tasks.filter { task ->
            val matchesStatus = when (state.statusFilter) {
                TaskStatusFilter.ALL -> true
                TaskStatusFilter.PENDING -> !task.completed
                TaskStatusFilter.COMPLETED -> task.completed
            }
            val matchesCategory = state.categoryFilterId == null ||
                task.categoryId == state.categoryFilterId
            matchesStatus && matchesCategory
        }

    fun setStatusFilter(filter: TaskStatusFilter) {
        statusFilter.value = filter
    }

    fun setCategoryFilter(categoryId: Long?) {
        categoryFilterId.value = categoryId
    }

    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            val newCompleted = !task.completed
            repository.setTaskCompleted(task.id, newCompleted)
            reminders.onTaskChanged(task.copy(completed = newCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
            reminders.onTaskDeleted(task.id)
        }
    }
}