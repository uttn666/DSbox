package com.dssrkh.app.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dssrkh.app.DssrkhApp
import com.dssrkh.app.data.db.MemoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(characterId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as DssrkhApp
    val memories by app.database.memoryDao()
        .observeByCharacter(characterId)
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf("active") }

    val filtered = when (filter) {
        "faded" -> memories.filter { it.status == "faded" }
        "all" -> memories
        else -> memories.filter { it.status == "active" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记忆") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = filter == "active", onClick = { filter = "active" }, label = { Text("生效") })
                FilterChip(selected = filter == "faded", onClick = { filter = "faded" }, label = { Text("已淡忘") })
                FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("全部") })
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "还没有记忆",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { memory ->
                        MemoryCard(
                            memory = memory,
                            onPin = {
                                scope.launch {
                                    app.database.memoryDao().update(memory.copy(pinned = !memory.pinned))
                                }
                            },
                            onRevive = {
                                scope.launch {
                                    app.database.memoryDao().update(memory.copy(status = "active"))
                                }
                            },
                            onDelete = {
                                scope.launch { app.database.memoryDao().deleteById(memory.id) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(
    memory: MemoryEntity,
    onPin: () -> Unit,
    onRevive: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = kindLabel(memory.kind),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "重要 ${memory.importance}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (memory.status != "active") {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (memory.status == "faded") "已淡忘" else memory.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onPin) {
                    Icon(
                        if (memory.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (memory.pinned) "取消置顶" else "置顶",
                    )
                }
                if (memory.status != "active") {
                    IconButton(onClick = onRevive) {
                        Icon(Icons.Filled.Refresh, contentDescription = "唤回")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(memory.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatTime(memory.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun kindLabel(kind: String): String = when (kind) {
    "user_emotion" -> "情绪"
    "event" -> "事件"
    "self_feel" -> "感受"
    "fact" -> "事实"
    "promise" -> "约定"
    else -> kind
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
