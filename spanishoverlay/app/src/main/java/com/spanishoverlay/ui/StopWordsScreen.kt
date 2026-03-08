package com.spanishoverlay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StopWordsScreen(initialWords: List<String>, onWordsChanged: (List<String>) -> Unit) {
    var words by remember { mutableStateOf(initialWords) }
    var input by remember { mutableStateOf("") }

    fun addWord() {
        val w = input.lowercase().trim()
        if (w.isNotBlank() && w !in words) {
            words = (words + w).sorted()
            onWordsChanged(words)
            input = ""
        }
    }

    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it.lowercase().trim() },
                modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Add stop word") },
                keyboardActions = KeyboardActions(onDone = { addWord() }))
            IconButton(onClick = ::addWord) { Icon(Icons.Default.Add, "Add") }
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(words, key = { it }) { word ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(word, Modifier.weight(1f).padding(start = 4.dp))
                    IconButton(onClick = { words = words - word; onWordsChanged(words) }) {
                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
