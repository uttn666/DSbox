package com.dssrkh.app.ui.characters

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.dssrkh.app.data.ImageStore
import com.dssrkh.app.data.TextImport
import com.dssrkh.app.data.db.CharacterEntity
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun CharacterFormDialog(
    title: String,
    initial: CharacterEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, persona: String, opening: String, avatarPath: String?, bgPath: String?) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var persona by remember { mutableStateOf(initial?.personaText ?: "") }
    var opening by remember { mutableStateOf(initial?.openingMessage ?: "") }
    var avatarPath by remember { mutableStateOf(initial?.avatarPath) }
    var bgPath by remember { mutableStateOf(initial?.bgOverride) }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<TextImport.Imported?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val imageRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ImageStore.saveImage(context, uri)?.let { avatarPath = it.path }
            }
        }
    }
    val backgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ImageStore.saveImage(context, uri)?.let { bgPath = it.path }
            }
        }
    }
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                TextImport.read(context, uri)
                    .onSuccess {
                        pendingImport = it
                        importError = null
                    }
                    .onFailure {
                        importError = it.message ?: "导入失败"
                    }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭")
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            onSave(name.trim(), persona.trim(), opening.trim(), avatarPath, bgPath)
                        },
                        enabled = name.isNotBlank(),
                    ) {
                        Text("保存")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(88.dp)
                            .clip(CircleShape)
                            .clickable { avatarPicker.launch(imageRequest) },
                    ) {
                        if (!avatarPath.isNullOrBlank()) {
                            AsyncImage(
                                model = File(avatarPath!!),
                                contentDescription = "角色头像",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        TextButton(onClick = { avatarPicker.launch(imageRequest) }) {
                            Text(if (avatarPath == null) "添加头像" else "更换头像")
                        }
                        if (avatarPath != null) {
                            TextButton(onClick = { avatarPath = null }) {
                                Text("移除头像", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("角色名") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "角色设定",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                documentPicker.launch(
                                    arrayOf(
                                        "text/*",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/pdf",
                                        "*/*",
                                    ),
                                )
                            },
                        ) {
                            Text("导入文件")
                        }
                    }
                    OutlinedTextField(
                        value = persona,
                        onValueChange = { persona = it },
                        label = { Text("性格、背景、说话方式…") },
                        minLines = 8,
                        maxLines = 16,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "支持导入 txt、md、docx，自动识别 UTF-8 与 GBK",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (importError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = importError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = opening,
                        onValueChange = { opening = it },
                        label = { Text("开场白（可选）") },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(20.dp))

                    Text("聊天背景", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    if (!bgPath.isNullOrBlank()) {
                        Box {
                            AsyncImage(
                                model = File(bgPath!!),
                                contentDescription = "聊天背景",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                            IconButton(
                                onClick = { bgPath = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(30.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "移除背景",
                                        modifier = Modifier.padding(6.dp),
                                    )
                                }
                            }
                        }
                        TextButton(onClick = { backgroundPicker.launch(imageRequest) }) {
                            Text("更换背景")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { backgroundPicker.launch(imageRequest) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("选择背景图")
                        }
                    }

                    if (onDelete != null) {
                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Text("删除角色")
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    pendingImport?.let { imported ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("导入角色设定") },
            text = {
                Column {
                    Text("已读取：${imported.name}")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "共 ${imported.text.length} 字。选择追加到现有设定，或直接替换。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = imported.text,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        persona = if (persona.isBlank()) {
                            imported.text
                        } else {
                            persona.trimEnd() + "\n\n" + imported.text
                        }
                        pendingImport = null
                    },
                ) { Text("追加") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            persona = imported.text
                            pendingImport = null
                        },
                    ) { Text("替换") }
                    TextButton(onClick = { pendingImport = null }) { Text("取消") }
                }
            },
        )
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除角色") },
            text = { Text("该角色的所有会话和记忆都会一起删除，且无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }
}
