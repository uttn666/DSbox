package com.dssrkh.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dssrkh.app.BuildConfig
import com.dssrkh.app.DssrkhApp
import com.dssrkh.app.data.SettingsStore
import com.dssrkh.app.ui.components.QqGroupDialog
import com.dssrkh.app.ui.imports.ShareImportDialog

private enum class SettingsDialog {
    ApiKey,
    Model,
    Effort,
    Profile,
    Reminder,
    Agreement,
    Help,
    Data,
    ClearKey,
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenUsage: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as DssrkhApp
    val settings = app.settings
    val dark = isSystemInDarkTheme()

    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var key by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.defaultModel) }
    var effort by remember { mutableStateOf(settings.defaultEffort) }
    var thinking by remember { mutableStateOf(settings.thinkingEnabled) }
    var showReasoning by remember { mutableStateOf(settings.showReasoning) }
    var shareProfile by remember { mutableStateOf(settings.shareProfile) }
    var profileText by remember { mutableStateOf(settings.profileText) }
    var webSearch by remember { mutableStateOf(settings.webSearchEnabled) }
    var readLinks by remember { mutableStateOf(settings.readLinks) }
    var reminder by remember { mutableStateOf(settings.reminderMinutes) }
    var showShareImport by remember { mutableStateOf(false) }
    var showQqGroup by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = if (dark) MaterialTheme.colorScheme.background else Color(0xFFF7F7F7),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SettingsTopBar(onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                SettingsSection("账户") {
                    SettingsRow(
                        icon = Icons.Filled.Key,
                        title = "API Key",
                        value = if (key.isBlank()) "未设置" else "已设置",
                        onClick = { dialog = SettingsDialog.ApiKey },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Filled.Storage,
                        title = "数据管理",
                        onClick = { dialog = SettingsDialog.Data },
                    )
                }

                SettingsSection("应用") {
                    SettingsRow(
                        icon = Icons.Filled.Tune,
                        title = "默认模型",
                        value = if (model == SettingsStore.MODEL_FLASH) "deepseek-flash" else "deepseek-v4-pro",
                        onClick = { dialog = SettingsDialog.Model },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Filled.Psychology,
                        title = "思考强度",
                        value = effort,
                        onClick = { dialog = SettingsDialog.Effort },
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        icon = Icons.Filled.Public,
                        title = "联网搜索",
                        checked = webSearch,
                        onChange = {
                            webSearch = it
                            settings.webSearchEnabled = it
                        },
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        icon = Icons.Filled.Link,
                        title = "阅读链接",
                        checked = readLinks,
                        enabled = webSearch,
                        onChange = {
                            readLinks = it
                            settings.readLinks = it
                        },
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        icon = Icons.Filled.Visibility,
                        title = "显示思考过程",
                        checked = showReasoning,
                        onChange = {
                            showReasoning = it
                            settings.showReasoning = it
                        },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Filled.Person,
                        title = "用户档案",
                        value = if (shareProfile) "已共享" else "仅角色",
                        onClick = { dialog = SettingsDialog.Profile },
                    )
                }

                SettingsSection("通用") {
                    SettingsRow(
                        icon = Icons.Filled.AccessTime,
                        title = "连续使用提醒",
                        value = if (reminder == 0) "已关闭" else "${reminder / 60} 小时",
                        onClick = { dialog = SettingsDialog.Reminder },
                    )
                }

                SettingsSection("关于") {
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        title = "版本",
                        value = BuildConfig.VERSION_NAME,
                        showChevron = false,
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Filled.Description,
                        title = "服务协议",
                        onClick = { dialog = SettingsDialog.Agreement },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Filled.HelpOutline,
                        title = "帮助与反馈",
                        onClick = { dialog = SettingsDialog.Help },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Filled.Groups,
                        title = "加入交流群",
                        value = "QQ 339858034",
                        onClick = { showQqGroup = true },
                    )
                }

                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (dark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    SettingsRow(
                        icon = Icons.Filled.Delete,
                        title = "清除 API Key",
                        showChevron = false,
                        danger = true,
                        onClick = { dialog = SettingsDialog.ClearKey },
                    )
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    when (dialog) {
        SettingsDialog.ApiKey -> ApiKeyDialog(
            initial = key,
            onDismiss = { dialog = null },
            onSave = {
                key = it
                settings.apiKey = it
                dialog = null
            },
        )
        SettingsDialog.Model -> ChoiceDialog(
            title = "默认模型",
            options = listOf(
                "deepseek-flash" to "Flash（支持图片/文档/联网）",
                "deepseek-v4-pro" to "V4 Pro",
            ),
            selected = model,
            onSelect = {
                model = it
                settings.defaultModel = it
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Effort -> ChoiceDialog(
            title = "思考强度",
            options = listOf(
                "low" to "low",
                "high" to "high",
                "max" to "max",
            ),
            selected = effort,
            onSelect = {
                effort = it
                settings.defaultEffort = it
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Profile -> ProfileDialog(
            share = shareProfile,
            text = profileText,
            onShareChange = {
                shareProfile = it
                settings.shareProfile = it
            },
            onTextChange = {
                profileText = it
                settings.profileText = it
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Reminder -> ChoiceDialog(
            title = "连续使用提醒",
            options = listOf(
                "0" to "关闭",
                "60" to "1 小时",
                "120" to "2 小时",
                "180" to "3 小时",
            ),
            selected = reminder.toString(),
            onSelect = {
                val value = it.toIntOrNull() ?: 120
                reminder = value
                settings.reminderMinutes = value
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Agreement -> InfoDialog(
            title = "服务协议",
            text = "服务协议文本待补充。使用本应用需遵守 DeepSeek 开放平台服务协议；AI 生成内容仅供参考。",
            onDismiss = { dialog = null },
        )
        SettingsDialog.Help -> InfoDialog(
            title = "帮助与反馈",
            text = "反馈渠道待定。当前为开发版，可在项目仓库提交问题。",
            onDismiss = { dialog = null },
        )
        SettingsDialog.Data -> DataDialog(
            onUsage = {
                dialog = null
                onOpenUsage()
            },
            onImportShare = {
                dialog = null
                showShareImport = true
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.ClearKey -> ClearKeyDialog(
            onDismiss = { dialog = null },
            onConfirm = {
                settings.apiKey = ""
                key = ""
                dialog = null
            },
        )
        null -> Unit
    }

    if (showShareImport) {
        ShareImportDialog(
            onDismiss = { showShareImport = false },
            onImported = { sessionId ->
                showShareImport = false
                onOpenSession(sessionId)
            },
        )
    }

    if (showQqGroup) {
        QqGroupDialog(onDismiss = { showQqGroup = false })
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = "设置",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 28.dp, top = 18.dp, bottom = 8.dp),
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSystemInDarkTheme()) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    showChevron: Boolean = true,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickable)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (showChevron) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ApiKeyDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    var visible by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("API Key") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("sk-…") },
                supportingText = { Text("只保存在本机加密存储，不会上传") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                visualTransformation = if (visible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (visible) "隐藏" else "显示",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(value.trim()) },
                enabled = value.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(
                                color = if (selected == value) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelect(value) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == value, onClick = { onSelect(value) })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun ProfileDialog(
    share: Boolean,
    text: String,
    onShareChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("用户档案") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("在所有角色间共享")
                        Text(
                            "关闭时只使用每个角色自己的用户人设",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = share, onCheckedChange = onShareChange)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text("称呼、自我介绍、偏好") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}

@Composable
private fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title) },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
    )
}

@Composable
private fun DataDialog(
    onUsage: () -> Unit,
    onImportShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("数据管理") },
        text = {
            Column {
                Text(
                    text = "用量与费用",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onUsage)
                        .padding(vertical = 12.dp),
                )
                Text(
                    text = "从分享链接导入",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onImportShare)
                        .padding(vertical = 12.dp),
                )
                Text(
                    text = "备份与恢复（开发中）",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Text(
                    text = "导入官方会话（等待导出样本）",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun ClearKeyDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("清除 API Key") },
        text = { Text("清除后需要重新填写 API Key 才能继续对话，授权状态与本地数据不受影响。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("清除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
