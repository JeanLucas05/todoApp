package br.edu.ifpe.todo.domain

data class Category(
    val id: Long,
    val name: String,
)

data class Task(
    val id: Long,
    val title: String,
    val description: String?,
    val completed: Boolean,
    val dueAtMillis: Long?,
    val createdAt: Long,
    val categoryId: Long?,
    val categoryName: String?,
)

enum class TaskStatusFilter(val label: String) {
    ALL("Todas"),
    PENDING("Pendentes"),
    COMPLETED("Concluídas"),
}