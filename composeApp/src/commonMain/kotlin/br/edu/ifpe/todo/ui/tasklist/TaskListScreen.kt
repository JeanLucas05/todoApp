package br.edu.ifpe.todo.ui.tasklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.edu.ifpe.todo.AppContainer
import br.edu.ifpe.todo.domain.Task
import br.edu.ifpe.todo.domain.TaskStatusFilter
import br.edu.ifpe.todo.notifications.ReminderCoordinator
import br.edu.ifpe.todo.notifications.ReminderScheduler
import br.edu.ifpe.todo.ui.components.CategoryPill
import br.edu.ifpe.todo.ui.components.EmptyState
import br.edu.ifpe.todo.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onOpenTask: (Long) -> Unit,
    onCreateTask: () -> Unit,
    onManageCategories: () -> Unit,
) {
    val viewModel: TaskListViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TaskListViewModel(
                    repository = AppContainer.repository,
                    reminders = ReminderCoordinator(ReminderScheduler()),
                )
            }
        },
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tasks = viewModel.filteredTasks(uiState)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tarefas") },
                actions = {
                    TextButton(onClick = onManageCategories) {
                        Text("Categorias")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTask) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StatusFilterRow(
                selected = uiState.statusFilter,
                onSelect = viewModel::setStatusFilter,
            )
            CategoryFilterRow(
                uiState = uiState,
                onSelect = viewModel::setCategoryFilter,
            )
            HorizontalDivider()

            if (tasks.isEmpty()) {
                EmptyState(
                    text = if (uiState.tasks.isEmpty()) {
                        "Nenhuma tarefa ainda.\nToque em + para criar uma."
                    } else {
                        "Nenhuma tarefa corresponde aos filtros."
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggleCompleted(task) },
                            onClick = { onOpenTask(task.id) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterRow(
    selected: TaskStatusFilter,
    onSelect: (TaskStatusFilter) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TaskStatusFilter.entries.forEachIndexed { _, filter ->
            SegmentedButton(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                shape = SegmentedButtonDefaults.itemShape(index = filter.ordinal, count = TaskStatusFilter.entries.size),
            ) {
                Text(filter.label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    uiState: TaskListUiState,
    onSelect: (Long?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = uiState.categoryFilterId == null,
            onClick = { onSelect(null) },
            label = { Text("Todas as categorias") },
        )
        uiState.categories.forEach { category ->
            FilterChip(
                selected = uiState.categoryFilterId == category.id,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (task.description?.isNotBlank() == true) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    task.categoryName?.let { CategoryPill(it) }
                    task.dueAtMillis?.let { due ->
                        Text(
                            text = "Vence ${formatDateTime(due)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}