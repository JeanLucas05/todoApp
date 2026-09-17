package br.edu.ifpe.todo.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import br.edu.ifpe.todo.data.db.TodoDatabase
import br.edu.ifpe.todo.data.db.Task as TaskRow
import br.edu.ifpe.todo.data.db.Category as CategoryRow
import br.edu.ifpe.todo.domain.Task
import br.edu.ifpe.todo.domain.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TodoRepository(private val database: TodoDatabase) {

    private fun queries() = database.todoQueries

    private fun TaskRow.toDomain(categoryNames: Map<Long, String>) = Task(
        id = id,
        title = title,
        description = description,
        completed = completed != 0L,
        dueAtMillis = due_at,
        createdAt = created_at,
        categoryId = category_id,
        categoryName = category_id?.let { categoryNames[it] },
    )

    private fun CategoryRow.toDomain() = Category(id = id, name = name)

    fun observeTasks(): Flow<List<Task>> {
        val tasksFlow: Flow<List<Task>> = queries()
            .selectAllTasks()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain(emptyMap()) } }

        val categoriesFlow: Flow<List<Category>> =
            queries().selectAllCategories().asFlow().mapToList(Dispatchers.Default).map { rows ->
                rows.map { it.toDomain() }
            }

        return combine(tasksFlow, categoriesFlow) { taskList, categoryList ->
            val names = categoryList.associateBy({ it.id }, { it.name })
            taskList.map { it.copy(categoryName = it.categoryId?.let { id -> names[id] }) }
        }
    }

    fun observeCategories(): Flow<List<Category>> =
        queries().selectAllCategories().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    fun fetchTask(taskId: Long): Task? {
        val row = queries().selectTaskById(taskId).executeAsOneOrNull() ?: return null
        val names = queries()
            .selectAllCategories()
            .executeAsList()
            .associateBy({ it.id }, { it.name })
        return row.toDomain(names)
    }

    fun insertTask(
        title: String,
        description: String?,
        dueAtMillis: Long?,
        categoryId: Long?,
        createdMillis: Long,
    ): Long {
        queries().insertTask(
            title = title,
            description = description,
            completed = 0L,
            due_at = dueAtMillis,
            created_at = createdMillis,
            category_id = categoryId,
        )
        return lastInsertRowId()
    }

    fun updateTask(
        taskId: Long,
        title: String,
        description: String?,
        completed: Boolean,
        dueAtMillis: Long?,
        categoryId: Long?,
    ) {
        queries().updateTask(
            title = title,
            description = description,
            completed = if (completed) 1L else 0L,
            due_at = dueAtMillis,
            category_id = categoryId,
            id = taskId,
        )
    }

    fun setTaskCompleted(taskId: Long, completed: Boolean) {
        queries().setTaskCompleted(if (completed) 1L else 0L, taskId)
    }

    fun deleteTask(taskId: Long) {
        queries().deleteTask(taskId)
    }

    fun insertCategory(name: String): Long {
        queries().insertCategory(name)
        return lastInsertRowId()
    }

    fun renameCategory(categoryId: Long, name: String) {
        queries().updateCategoryName(name, categoryId)
    }

    fun deleteCategory(categoryId: Long) {
        queries().deleteCategory(categoryId)
    }

    private fun lastInsertRowId(): Long =
        queries().selectLastInsertRowid().executeAsOne()
}