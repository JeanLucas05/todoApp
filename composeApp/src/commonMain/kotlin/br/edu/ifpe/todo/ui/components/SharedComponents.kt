package br.edu.ifpe.todo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun CategoryPill(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun SectionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
fun OverdueDueText(text: String, isOverdue: Boolean, modifier: Modifier = Modifier) {
    val color = if (isOverdue) MaterialTheme.colorScheme.error else Color.Unspecified
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
        textDecoration = if (isOverdue) TextDecoration.None else TextDecoration.None,
    )
}

@Composable
fun Strikethrough(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge,
        textDecoration = TextDecoration.LineThrough,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}