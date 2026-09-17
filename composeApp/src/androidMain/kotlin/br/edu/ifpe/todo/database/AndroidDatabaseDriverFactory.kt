package br.edu.ifpe.todo.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import br.edu.ifpe.todo.data.db.DatabaseDriverFactory
import br.edu.ifpe.todo.data.db.TodoDatabase

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = TodoDatabase.Schema,
            context = context,
            name = "todo.db",
        )
}