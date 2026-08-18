package com.giathinh.canlua.ui.screen.aichat

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class HandbookEntry(val title: String, val content: String, val keywords: List<String>)

@Composable
fun AiChatScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(emptyList<HandbookEntry>()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(context) { entries = loadEntries(context) }
    val filtered = remember(entries, query) {
        if (query.isBlank()) entries else entries.filter { entry ->
            entry.title.contains(query, true) || entry.content.contains(query, true) || entry.keywords.any { it.contains(query, true) }
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cam nang nong nghiep offline", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Tim kiem kien thuc") }, singleLine = true)
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.title }) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(entry.title, style = MaterialTheme.typography.titleMedium)
                        Text(entry.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private suspend fun loadEntries(context: Context): List<HandbookEntry> = withContext(Dispatchers.IO) {
    runCatching {
        val root = JSONObject(context.assets.open("agronomy_knowledge.json").bufferedReader().use { it.readText() })
        val array = root.optJSONArray("entries") ?: return@runCatching emptyList()
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val values = item.optJSONArray("keywords")
                val keywords = if (values == null) emptyList() else List(values.length()) { values.optString(it) }
                add(HandbookEntry(item.optString("title"), item.optString("content"), keywords))
            }
        }
    }.getOrDefault(emptyList())
}
