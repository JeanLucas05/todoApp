package br.edu.ifpe.todo.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpe.todo.data.TodoRepository
import br.edu.ifpe.todo.domain.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: TodoRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insertCategory(trimmed)
        }
    }

    fun renameCategory(category: Category, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.renameCategory(category.id, trimmed)
        }
    }

    /**
     * Deleting a category leaves its tasks uncategorized
     * (enforced by the FK `ON DELETE SET NULL` in the database and/or the explicit
     * repository behavior).
     */
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category.id)
        }
    }
}