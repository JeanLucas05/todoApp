package br.edu.ifpe.todo.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.edu.ifpe.todo.AppContainer
import br.edu.ifpe.todo.domain.Category
import br.edu.ifpe.todo.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onDone: () -> Unit) {
    val viewModel: CategoriesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CategoriesViewModel(repository = AppContainer.repository) }
        },
    )
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var creatingCategory by remember { mutableStateOf(false) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias") },
                actions = {
                    TextButton(onClick = onDone) { Text("Concluir") }
                },
            )
        },
        floatingActionButton = {
            TextButton(onClick = { creatingCategory = true }) {
                Text("+ Nova categoria", style = MaterialTheme.typography.titleMedium)
            }
        },
    ) { padding ->
        if (categories.isEmpty()) {
            EmptyState(
                text = "Nenhuma categoria ainda.\nCrie uma para organizar suas tarefas.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        onRename = { editingCategory = category },
                        onDelete = { deletingCategory = category },
                    )
                }
            }
        }
    }

    if (creatingCategory) {
        CategoryNameDialog(
            title = "Nova categoria",
            initialName = "",
            confirmLabel = "Criar",
            onConfirm = { name ->
                creatingCategory = false
                viewModel.addCategory(name)
            },
            onDismiss = { creatingCategory = false },
        )
    }

    editingCategory?.let { category ->
        CategoryNameDialog(
            title = "Renomear categoria",
            initialName = category.name,
            confirmLabel = "Salvar",
            onConfirm = { name ->
                editingCategory = null
                viewModel.renameCategory(category, name)
            },
            onDismiss = { editingCategory = null },
        )
    }

    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Excluir categoria") },
            text = {
                Text("A categoria \"${category.name}\" será excluída. As tarefas que a usam ficarão sem categoria.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingCategory = null
                        viewModel.deleteCategory(category)
                    },
                ) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRename) { Text("Renomear") }
            TextButton(onClick = onDelete) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CategoryNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}