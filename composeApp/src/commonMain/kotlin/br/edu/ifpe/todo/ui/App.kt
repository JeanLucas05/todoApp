package br.edu.ifpe.todo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.edu.ifpe.todo.ui.categories.CategoriesScreen
import br.edu.ifpe.todo.ui.taskeditor.TaskEditorScreen
import br.edu.ifpe.todo.ui.tasklist.TaskListScreen

/**
 * Screens of the app. Navigation is a simple state machine:
 * data transferred to the editor is only the task id (null = create).
 */
private sealed interface Screen {
    data object TaskList : Screen
    data class TaskEditor(val taskId: Long?) : Screen
    data object Categories : Screen
}

@Composable
fun App() {
    MaterialTheme {
        var current by remember { mutableStateOf<Screen>(Screen.TaskList) }

        when (val screen = current) {
            Screen.TaskList -> TaskListScreen(
                onOpenTask = { taskId -> current = Screen.TaskEditor(taskId) },
                onCreateTask = { current = Screen.TaskEditor(null) },
                onManageCategories = { current = Screen.Categories },
            )

            is Screen.TaskEditor -> TaskEditorScreen(
                taskId = screen.taskId,
                onCancel = { current = Screen.TaskList },
                onSaved = { current = Screen.TaskList },
                onDeleted = { current = Screen.TaskList },
            )

            Screen.Categories -> CategoriesScreen(
                onDone = { current = Screen.TaskList },
            )
        }
    }
}