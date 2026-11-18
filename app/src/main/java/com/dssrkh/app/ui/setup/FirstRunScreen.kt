package com.dssrkh.app.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.dssrkh.app.data.SessionFactory
import com.dssrkh.app.data.db.CharacterEntity
import com.dssrkh.app.ui.characters.CharacterFormDialog
import com.dssrkh.app.ui.components.DsboxBrand
import kotlinx.coroutines.launch

@Composable
fun FirstRunScreen(onCreated: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as DssrkhApp
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            DsboxBrand()
            Spacer(Modifier.height(24.dp))
            Text(
                text = "先创建一个角色，才能开始对话",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { showCreate = true },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("创建角色")
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showCreate) {
        CharacterFormDialog(
            title = "新建角色",
            initial = null,
            onDismiss = { showCreate = false },
            onSave = { name, persona, opening, avatarPath, bgPath ->
                showCreate = false
                scope.launch {
                    val now = System.currentTimeMillis()
                    val entity = CharacterEntity(
                        name = name,
                        personaText = persona,
                        openingMessage = opening.ifBlank { null },
                        avatarPath = avatarPath,
                        bgOverride = bgPath,
                        createdAt = now,
                        updatedAt = now,
                    )
                    val id = app.database.characterDao().insert(entity)
                    val sessionId = SessionFactory.create(app.database, entity.copy(id = id))
                    onCreated(sessionId)
                }
            },
        )
    }
}
