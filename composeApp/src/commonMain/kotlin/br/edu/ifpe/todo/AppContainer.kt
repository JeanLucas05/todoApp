package br.edu.ifpe.todo

import app.cash.sqldelight.db.SqlDriver
import br.edu.ifpe.todo.data.TodoRepository
import br.edu.ifpe.todo.data.db.TodoDatabase

/**
 * Minimal dependency container for the whole app.
 * It is initialized from each platform entry point (Android: MainActivity,
 * iOS: MainViewController) with a platform-specific driver.
 */
object AppContainer {
    private var initialized = false
    lateinit var repository: TodoRepository
        private set

    fun init(driver: SqlDriver) {
        check(!initialized) { "AppContainer is already initialized" }
        initialized = true
        val database = TodoDatabase(driver)
        repository = TodoRepository(database)
    }
}