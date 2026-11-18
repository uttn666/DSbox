package com.dssrkh.app.ui.imports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dssrkh.app.data.SettingsStore
import com.dssrkh.app.data.db.MessageEntity
import com.dssrkh.app.data.db.SessionEntity
import com.dssrkh.app.data.share.ShareImportService
import kotlinx.coroutines.launch

@Composable
fun ShareImportDialog(
    initialCharacterId: Long? = null,
    onDismiss: () -> Unit,
    onImported: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as DssrkhApp
    val scope = rememberCoroutineScope()
    val service = remember { ShareImportService() }
    val characters by app.database.characterDao().observeAll().collectAsState(initial = emptyList())

    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var parsed by remember { mutableStateOf<ShareImportService.ImportedConversation?>(null) }
    var selectedCharacterId by remember {
        mutableStateOf(initialCharacterId ?: characters.firstOrNull()?.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("导入分享会话") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "粘贴 DeepSeek 分享链接，读取标题、思考过程和消息内容。图片与文件不会下载，会以文字占位。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = {
                        link = it
                        error = null
                    },
                    placeholder = { Text("https://chat.deepseek.com/share/…") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                service.fetch(link)
                                    .onSuccess {
                                        parsed = it
                                        if (selectedCharacterId == null) {
                                            selectedCharacterId = characters.firstOrNull()?.id
                                        }
                                    }
                                    .onFailure { error = it.message ?: "读取失败" }
                                loading = false
                            }
                        },
                        enabled = !loading && link.isNotBlank(),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(if (loading) "解析中…" else "解析链接")
                    }
                    if (loading) {
                        Spacer(Modifier.height(0.dp))
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .height(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                val parsedConversation = parsed
                if (parsedConversation != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = parsedConversation.title.ifBlank { "分享会话" },
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${parsedConversation.messages.size} 条消息 · ${parsedConversation.modelType ?: "默认模型"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("导入到角色", style = MaterialTheme.typography.labelMedium)
                    if (characters.isEmpty()) {
                        Text(
                            text = "还没有角色，请先在侧边栏新建角色。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        characters.forEach { character ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCharacterId = character.id }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = selectedCharacterId == character.id,
                                    onClick = { selectedCharacterId = character.id },
                                )
                                Text(character.name)
                            }
                        }
                    }
                }

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val conversation = parsed ?: return@TextButton
                    val characterId = selectedCharacterId ?: return@TextButton
                    scope.launch {
                        importing = true
                        val now = System.currentTimeMillis()
                        val title = resolveTitle(conversation)
                        val sessionId = app.database.sessionDao().insert(
                            SessionEntity(
                                characterId = characterId,
                                title = title,
                                modelOverride = if (
                                    conversation.modelType?.contains("pro", ignoreCase = true) == true
                                ) {
                                    SettingsStore.MODEL_PRO
                                } else {
                                    null
                                },
                                createdAt = now,
                                updatedAt = now,
                            ),
                        )
                        conversation.messages.forEachIndexed { index, message ->
                            val suffix = if (message.fileNames.isNotEmpty()) {
                                "\n\n[附件：${message.fileNames.joinToString("、")}]"
                            } else {
                                ""
                            }
                            val text = (message.content + suffix).trim()
                            app.database.messageDao().insert(
                                MessageEntity(
                                    sessionId = sessionId,
                                    role = message.role.lowercase(),
                                    rawText = text,
                                    displayText = text,
                                    reasoningText = message.thinking,
                                    blocksJson = if (message.role == "ASSISTANT") {
                                        """{"importedOfficial":true}"""
                                    } else {
                                        null
                                    },
                                    model = SettingsStore.MODEL_FLASH,
                                    status = "done",
                                    createdAt = now + index + 1,
                                ),
                            )
                        }
                        importing = false
                        onImported(sessionId)
                    }
                },
                enabled = parsed != null && selectedCharacterId != null && !importing,
            ) {
                Text(if (importing) "导入中…" else "导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun resolveTitle(conversation: ShareImportService.ImportedConversation): String {
    val title = conversation.title.trim()
    if (title.isNotBlank() &&
        !title.equals("Shared Conversation", ignoreCase = true) &&
        title != "分享的对话"
    ) {
        return title
    }
    return conversation.messages
        .firstOrNull { it.role == "USER" }
        ?.content
        ?.replace('\n', ' ')
        ?.take(20)
        ?.trim()
        ?.ifBlank { "导入的会话" }
        ?: "导入的会话"
}
