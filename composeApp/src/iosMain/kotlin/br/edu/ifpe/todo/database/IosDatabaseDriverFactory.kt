package br.edu.ifpe.todo.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import br.edu.ifpe.todo.data.db.DatabaseDriverFactory
import br.edu.ifpe.todo.data.db.TodoDatabase

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        NativeSqliteDriver(TodoDatabase.Schema, "todo.db")
}