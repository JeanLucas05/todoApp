package br.edu.ifpe.todo

import android.content.Context
import br.edu.ifpe.todo.database.AndroidDatabaseDriverFactory
import br.edu.ifpe.todo.notifications.createReminderChannel

/** Platform setup for Android, called from MainActivity before setContent(). */
fun initializeApp(context: Context) {
    val appContext = context.applicationContext
    AndroidAppContext.attach(appContext)
    val driver = AndroidDatabaseDriverFactory(appContext).createDriver()
    AppContainer.init(driver)
    createReminderChannel(appContext)
}