package br.edu.ifpe.todo.ui.taskeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.edu.ifpe.todo.AppContainer
import br.edu.ifpe.todo.notifications.ReminderCoordinator
import br.edu.ifpe.todo.notifications.ReminderScheduler
import br.edu.ifpe.todo.util.TaskDueUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    taskId: Long?,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
) {
    val viewModel: TaskEditorViewModel = viewModel(
        key = "taskEditor",
        factory = viewModelFactory {
            initializer {
                TaskEditorViewModel(
                    taskId = taskId,
                    repository = AppContainer.repository,
                    reminders = ReminderCoordinator(ReminderScheduler()),
                )
            }
        },
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewTask) "Nova tarefa" else "Editar tarefa") },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Cancelar") }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onSaved) },
                        enabled = uiState.canSave,
                    ) { Text("Salvar") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título (obrigatório)") },
                singleLine = true,
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descrição (opcional)") },
                minLines = 3,
            )

            CategoryDropdown(
                selectedId = uiState.categoryId,
                categories = uiState.categories,
                onSelect = viewModel::onCategoryChange,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val dueText = buildDueText(uiState.dueDate, uiState.dueTime)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Data de vencimento", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = dueText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { showDatePicker = true }, enabled = true) {
                    Text(if (uiState.dueDate == null) "Definir data" else "Alterar data")
                }
                if (uiState.dueDate != null) {
                    TextButton(onClick = { showTimePicker = true }) { Text("Hora") }
                }
                if (uiState.dueDate != null || uiState.dueTime != null) {
                    TextButton(onClick = viewModel::clearDueDateTime) { Text("Limpar") }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = uiState.completed,
                    onCheckedChange = viewModel::onCompletedChange,
                )
                Text("Tarefa concluída")
            }

            if (!uiState.isNewTask) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Excluir tarefa", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dueDate?.let(TaskDueUtils::toDatePickerMillis),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        viewModel.onDueDateChange(
                            dateState.selectedDateMillis?.let(TaskDueUtils::fromDatePickerMillis)
                        )
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            uiState = uiState,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                showTimePicker = false
                viewModel.onDueTimeChange(time)
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir tarefa") },
            text = { Text("Tem certeza que deseja excluir esta tarefa? A notificação, se existir, será cancelada.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteTask(onDeleted)
                    },
                ) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    uiState: TaskEditorUiState,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val initialHour = uiState.dueTime?.hour ?: 12
    val initialMinute = uiState.dueTime?.minute ?: 0
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime(state.hour, state.minute)) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Selecione a hora")
                TimePicker(state = state)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedId: Long?,
    categories: List<br.edu.ifpe.todo.domain.Category>,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name ?: "Sem categoria"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria (opcional)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Sem categoria") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun buildDueText(date: LocalDate?, time: LocalTime?): String = when {
    date == null && time == null -> "Sem prazo definido."
    date != null && time != null ->
        "Vence em ${date.dayOfMonth}/${date.monthNumber}/${date.year} às ${time.hour}:${time.minute.toString().padStart(2, '0')}"
    date != null ->
        "Vence em ${date.dayOfMonth}/${date.monthNumber}/${date.year} às 23:59"
    else -> "Sem prazo definido."
}