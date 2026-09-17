package br.edu.ifpe.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.edu.ifpe.todo.ui.App

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        initializeApp(applicationContext)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App() }
    }

    override fun onResume() {
        super.onResume()
        AndroidAppContext.registerActivity(this)
    }

    override fun onPause() {
        AndroidAppContext.unregisterActivity(this)
        super.onPause()
    }
}