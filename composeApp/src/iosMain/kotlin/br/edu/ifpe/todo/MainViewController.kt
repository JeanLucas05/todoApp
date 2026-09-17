package br.edu.ifpe.todo

import androidx.compose.ui.window.ComposeUIViewController
import br.edu.ifpe.todo.database.IosDatabaseDriverFactory
import br.edu.ifpe.todo.ui.App
import platform.UIKit.UIViewController

@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController {
    AppContainer.init(IosDatabaseDriverFactory().createDriver())
    return ComposeUIViewController { App() }
}