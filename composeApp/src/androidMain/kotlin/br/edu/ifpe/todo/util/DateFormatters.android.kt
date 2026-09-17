package br.edu.ifpe.todo.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))