package com.dssrkh.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.dssrkh.app.DssrkhApp
import com.dssrkh.app.data.LocalAttachment
import com.dssrkh.app.data.SessionFactory
import com.dssrkh.app.data.SettingsStore
import com.dssrkh.app.data.db.CharacterEntity
import com.dssrkh.app.data.db.MessageEntity
import com.dssrkh.app.data.db.SessionEntity
import com.dssrkh.app.domain.SearchBlocks
import com.dssrkh.app.ui.characters.CharacterFormDialog
import com.dssrkh.app.ui.components.DsboxMark
import com.dssrkh.app.ui.components.MarkdownText
import java.io.File
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: Long,
    onOpenSession: (Long) -> Unit,
    onOpenMemories: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onFirstRun: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as DssrkhApp
    val vm: ChatViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ChatViewModel(app, sessionId) }
        },
    )
    val state = vm.state
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    val characterId = state.character?.id ?: 0L
    val sessions by remember(characterId) {
        app.database.sessionDao().observeByCharacter(characterId)
    }.collectAsState(initial = emptyList())
    val characters by app.database.characterDao().observeAll().collectAsState(initial = emptyList())
    var thinkingEnabled by remember { mutableStateOf(app.settings.thinkingEnabled) }
    var webSearchEnabled by remember { mutableStateOf(app.settings.webSearchEnabled) }
    var editingCharacter by remember { mutableStateOf<CharacterEntity?>(null) }
    var creatingCharacter by remember { mutableStateOf(false) }
    val greeting = remember { greetingText() }
    val createNewSession: () -> Unit = {
        scope.launch {
            drawerState.close()
            val character = state.character ?: return@launch
            val newId = SessionFactory.create(app.database, character)
            onOpenSession(newId)
        }
    }
    val listState = rememberLazyListState()
    val latest = state.messages.lastOrNull()
    val clipboard = LocalClipboardManager.current
    var editingMessage by remember { mutableStateOf<ChatMessageUi?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearNotice()
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9),
    ) { uris -> if (uris.isNotEmpty()) vm.addImages(uris) }
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> uris.forEach { vm.addDocument(it) } }
    val recordPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) vm.startRecording() else vm.showNotice("需要麦克风权限才能语音输入")
    }
    val onMic: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        when {
            !granted -> recordPermission.launch(Manifest.permission.RECORD_AUDIO)
            state.recording -> vm.stopRecording()
            else -> vm.startRecording()
        }
    }

    LaunchedEffect(
        state.messages.size,
        latest?.text?.length,
        latest?.reasoning?.length,
    ) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    val bgPath = state.character?.bgOverride
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    LaunchedEffect(imeBottom, state.messages.size) {
        if (imeBottom > 0 && state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex, scrollOffset = 100_000)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                characters = characters,
                currentCharacterId = characterId,
                sessions = sessions,
                currentSessionId = sessionId,
                onSelectCharacter = { character ->
                    scope.launch {
                        drawerState.close()
                        val existing = app.database.sessionDao().latest(character.id)
                        val target = existing?.id ?: SessionFactory.create(app.database, character)
                        onOpenSession(target)
                    }
                },
                onEditCharacter = { editingCharacter = it },
                onCreateCharacter = { creatingCharacter = true },
                onNewSession = createNewSession,
                onOpenSession = { id ->
                    scope.launch { drawerState.close() }
                    onOpenSession(id)
                },
                onOpenMemories = {
                    if (characterId != 0L) {
                        scope.launch { drawerState.close() }
                        onOpenMemories(characterId)
                    }
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!bgPath.isNullOrBlank()) {
                AsyncImage(
                    model = File(bgPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(6.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.78f)),
                )
            }
            Scaffold(
                containerColor = if (bgPath.isNullOrBlank()) {
                    MaterialTheme.colorScheme.background
                } else {
                    Color.Transparent
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.character?.name ?: "对话",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${modelLabel(state.model)} · ${effortLabel(state.effort)} · AI 生成内容",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "对话列表")
                    }
                },
                actions = {
                    IconButton(onClick = createNewSession) {
                        Icon(Icons.Filled.AddCircleOutline, contentDescription = "新对话")
                    }
                    ChatOptionsMenu(
                        model = state.model,
                        effort = state.effort,
                        onModel = vm::setModel,
                        onEffort = vm::setEffort,
                        onCompact = vm::compactNow,
                    )
                },
            )
        },
        bottomBar = {
            InputBar(
                value = state.input,
                streaming = state.streaming,
                pendingImages = state.pendingImages,
                pendingFiles = state.pendingFiles,
                uploading = state.uploading,
                canAttach = state.model != SettingsStore.MODEL_PRO,
                recording = state.recording,
                onAttach = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onAttachDocument = { documentPicker.launch(arrayOf("*/*")) },
                onRemoveImage = vm::removePendingImage,
                onRemoveFile = vm::removePendingFile,
                onMic = onMic,
                thinking = thinkingEnabled,
                webSearch = webSearchEnabled,
                webSearchAvailable = state.model == SettingsStore.MODEL_FLASH,
                onToggleThinking = {
                    thinkingEnabled = !thinkingEnabled
                    app.settings.thinkingEnabled = thinkingEnabled
                    vm.showNotice(if (thinkingEnabled) "已开启深度思考" else "已关闭深度思考")
                },
                onToggleWebSearch = {
                    if (state.model != SettingsStore.MODEL_FLASH) {
                        vm.showNotice("deepseek-v4-pro 不支持联网搜索，请切换到 Flash")
                    } else {
                        webSearchEnabled = !webSearchEnabled
                        app.settings.webSearchEnabled = webSearchEnabled
                        vm.showNotice(
                            if (webSearchEnabled) {
                                "已开启智能搜索（按次计费）"
                            } else {
                                "已关闭智能搜索"
                            },
                        )
                    }
                },
                onValueChange = vm::updateInput,
                onSend = vm::send,
                onStop = vm::stop,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (state.messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    DsboxMark(size = 56.dp)
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageRow(
                            message = message,
                            characterName = state.character?.name.orEmpty(),
                            characterAvatar = state.character?.avatarPath,
                            canRegenerate = !state.streaming &&
                                message.role == "assistant" &&
                                state.messages.lastOrNull()?.id == message.id,
                            onCopy = {
                                clipboard.setText(AnnotatedString(message.text))
                                vm.showNotice("已复制")
                            },
                            onRegenerate = { vm.regenerate(message.id) },
                            onEdit = { editingMessage = message },
                            onDelete = { vm.deleteMessage(message.id) },
                        )
                    }
                    state.error?.let { error ->
                        item {
                            ErrorBanner(text = error, onDismiss = vm::clearError)
                        }
                    }
                }
            }
        }
    }

    editingMessage?.let { target ->
        EditMessageDialog(
            initial = target.text,
            onDismiss = { editingMessage = null },
            onConfirm = { text ->
                editingMessage = null
                vm.editUserMessage(target.id, text)
            },
        )
    }

    editingCharacter?.let { target ->
        CharacterFormDialog(
            title = "编辑角色",
            initial = target,
            onDismiss = { editingCharacter = null },
            onSave = { name, persona, opening, avatarPath, bgPath ->
                editingCharacter = null
                scope.launch {
                    app.database.characterDao().update(
                        target.copy(
                            name = name,
                            personaText = persona,
                            openingMessage = opening.ifBlank { null },
                            avatarPath = avatarPath,
                            bgOverride = bgPath,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                    if (target.id == state.character?.id) vm.refreshCharacter()
                }
            },
            onDelete = {
                editingCharacter = null
                scope.launch {
                    app.database.characterDao().deleteById(target.id)
                    val remaining = app.database.characterDao().observeAll().first()
                    val next = remaining.firstOrNull()
                    if (next == null) {
                        onFirstRun()
                    } else {
                        val targetSession = app.database.sessionDao().latest(next.id)?.id
                            ?: SessionFactory.create(app.database, next)
                        onOpenSession(targetSession)
                    }
                }
            },
        )
    }

    if (creatingCharacter) {
        CharacterFormDialog(
            title = "新建角色",
            initial = null,
            onDismiss = { creatingCharacter = false },
            onSave = { name, persona, opening, avatarPath, bgPath ->
                creatingCharacter = false
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
                    val session = SessionFactory.create(app.database, entity.copy(id = id))
                    onOpenSession(session)
                }
            },
        )
    }
    }
    }
}

@Composable
private fun ChatDrawer(
    characters: List<CharacterEntity>,
    currentCharacterId: Long,
    sessions: List<SessionEntity>,
    currentSessionId: Long,
    onSelectCharacter: (CharacterEntity) -> Unit,
    onEditCharacter: (CharacterEntity) -> Unit,
    onCreateCharacter: () -> Unit,
    onNewSession: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onOpenMemories: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val groups = remember(sessions, query) { groupSessions(sessions, query) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(320.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "搜索对话内容...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                item(key = "characters_header") {
                    Text(
                        text = "角色",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, top = 16.dp, bottom = 6.dp),
                    )
                }
                items(characters, key = { "char_${it.id}" }) { character ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectCharacter(character) }
                            .padding(horizontal = 6.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(30.dp),
                        ) {
                            if (!character.avatarPath.isNullOrBlank()) {
                                AsyncImage(
                                    model = File(character.avatarPath),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = character.name.take(1),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (character.id == currentCharacterId) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onEditCharacter(character) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "编辑角色",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item(key = "create_character") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onCreateCharacter)
                            .padding(horizontal = 6.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "新建角色",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                item(key = "sessions_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, top = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "对话",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = onNewSession,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "新对话",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                groups.forEach { group ->
                    item(key = "header_${group.label}") {
                        Text(
                            text = group.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 4.dp),
                        )
                    }
                    items(group.items, key = { "session_${it.id}" }) { session ->
                        DrawerSessionRow(
                            title = session.title.ifBlank { "未命名对话" },
                            selected = session.id == currentSessionId,
                            onClick = { onOpenSession(session.id) },
                        )
                    }
                }
                if (groups.isEmpty()) {
                    item {
                        Text(
                            text = "还没有对话",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DrawerAction(Icons.Filled.Psychology, "记忆", onOpenMemories)
            DrawerAction(Icons.Filled.Settings, "设置", onOpenSettings)
        }
    }
}
private data class SessionGroup(
    val label: String,
    val items: List<SessionEntity>,
    val sortKey: Long,
)

private fun groupSessions(sessions: List<SessionEntity>, query: String): List<SessionGroup> {
    val filtered = if (query.isBlank()) {
        sessions
    } else {
        sessions.filter { it.title.contains(query, ignoreCase = true) }
    }
    val now = System.currentTimeMillis()
    val recent = mutableListOf<SessionEntity>()
    val older = mutableMapOf<Pair<Int, Int>, MutableList<SessionEntity>>()
    filtered.forEach { session ->
        if (now - session.updatedAt <= 30L * 24 * 3600 * 1000) {
            recent += session
        } else {
            val date = Instant.ofEpochMilli(session.updatedAt).atZone(ZoneId.systemDefault())
            older.getOrPut(date.year to date.monthValue) { mutableListOf() } += session
        }
    }
    val groups = mutableListOf<SessionGroup>()
    if (recent.isNotEmpty()) {
        groups += SessionGroup("30 天内", recent, Long.MAX_VALUE)
    }
    older.entries
        .sortedByDescending { (key, _) -> key.first * 100L + key.second }
        .forEach { (key, items) ->
            groups += SessionGroup(
                label = "${key.first}年${key.second}月",
                items = items,
                sortKey = key.first * 100L + key.second,
            )
        }
    return groups
}

@Composable
private fun DrawerAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DrawerSessionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EditMessageDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑消息") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                minLines = 2,
                maxLines = 8,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) { Text("保存并重新生成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun MessageRow(
    message: ChatMessageUi,
    characterName: String,
    characterAvatar: String?,
    canRegenerate: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    if (message.role == "divider") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = message.text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        return
    }

    if (message.role == "user") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            if (message.attachments.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    message.attachments.forEach { attachment ->
                        if (attachment.kind == "image") {
                            AsyncImage(
                                model = File(attachment.path),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                tonalElevation = 1.dp,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.AttachFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = attachment.name.orEmpty(),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            if (message.text.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(0.86f),
                ) {
                    SelectionContainer {
                        Text(
                            text = message.text,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
            if (!message.streaming) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    MessageAction(Icons.Filled.ContentCopy, "复制", onCopy)
                    MessageAction(Icons.Filled.Edit, "编辑", onEdit)
                    MessageAction(Icons.Outlined.Delete, "删除", onDelete)
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(26.dp),
            ) {
                if (!characterAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = File(characterAvatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = characterName.take(1).ifBlank { "角" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = characterName.ifBlank { "角色" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (message.importedOfficial) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "来自官方会话",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        message.reasoning?.takeIf { it.isNotBlank() }?.let { reasoning ->
            ReasoningBlock(text = reasoning, streaming = message.streaming)
            Spacer(Modifier.height(8.dp))
        }

        if (message.text.isNotBlank()) {
            if (message.streaming) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                MarkdownText(text = message.text)
            }
        } else if (message.streaming) {
            Text(
                text = "正在回应…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val searchInfo = remember(message.blocksJson) { SearchBlocks.parse(message.blocksJson) }
        if (searchInfo != null) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = "已联网搜索 ${searchInfo.searchCount} 次 · 引用 ${searchInfo.citationCount} 个网页",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                searchInfo.titles.forEach { title ->
                    Text(
                        text = "· $title",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (message.role == "assistant" && message.memoryCount > 0) {
            Text(
                text = "已记住 ${message.memoryCount} 条",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        when (message.status) {
            "interrupted" -> Text(
                text = "已中断",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            "error" -> Text(
                text = "生成失败",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (!message.streaming) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                MessageAction(Icons.Filled.ContentCopy, "复制", onCopy)
                if (canRegenerate) {
                    MessageAction(Icons.Filled.Refresh, "重新生成", onRegenerate)
                }
                MessageAction(Icons.Outlined.Delete, "删除", onDelete)
            }
        }
    }
}

@Composable
private fun MessageAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReasoningBlock(text: String, streaming: Boolean) {
    var expanded by remember { mutableStateOf(true) }
    LaunchedEffect(streaming) {
        if (!streaming) expanded = false
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (streaming) "正在思考…" else "思考过程",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    streaming: Boolean,
    pendingImages: List<LocalAttachment>,
    pendingFiles: List<LocalAttachment>,
    uploading: Boolean,
    canAttach: Boolean,
    recording: Boolean,
    onAttach: () -> Unit,
    onAttachDocument: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onRemoveFile: (String) -> Unit,
    onMic: () -> Unit,
    thinking: Boolean,
    webSearch: Boolean,
    webSearchAvailable: Boolean,
    onToggleThinking: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    var attachMenu by remember { mutableStateOf(false) }
    val canSend = value.isNotBlank() || pendingImages.isNotEmpty() || pendingFiles.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (uploading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (pendingFiles.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(pendingFiles, key = { it.path }) { file ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                tonalElevation = 1.dp,
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.AttachFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = file.name.orEmpty().take(18),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                    )
                                    IconButton(
                                        onClick = { onRemoveFile(file.path) },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "移除",
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (pendingImages.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(pendingImages, key = { it.path }) { image ->
                            Box {
                                AsyncImage(
                                    model = File(image.path),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                IconButton(
                                    onClick = { onRemoveImage(image.path) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "移除",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = if (recording) "录音中…再点麦克风结束" else "发消息或按住说话",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    )
                    FilterChip(
                        selected = thinking,
                        onClick = onToggleThinking,
                        label = { Text("深度思考") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = chipColors,
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = webSearch,
                        onClick = onToggleWebSearch,
                        label = { Text("智能搜索") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Public,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = chipColors,
                    )
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton(
                            onClick = { attachMenu = true },
                            enabled = canAttach && !uploading,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "添加附件",
                                modifier = Modifier.size(24.dp),
                                tint = if (canAttach && !uploading) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            )
                        }
                        DropdownMenu(
                            expanded = attachMenu,
                            onDismissRequest = { attachMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("图片") },
                                onClick = {
                                    attachMenu = false
                                    onAttach()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("文档") },
                                onClick = {
                                    attachMenu = false
                                    onAttachDocument()
                                },
                            )
                        }
                    }
                    when {
                        streaming -> FilledIconButton(
                            onClick = onStop,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "停止")
                        }
                        canSend -> FilledIconButton(
                            onClick = onSend,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "发送")
                        }
                        else -> IconButton(onClick = onMic, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = if (recording) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = if (recording) "停止录音" else "语音输入",
                                tint = if (recording) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun ChatOptionsMenu(
    model: String,
    effort: String,
    onModel: (String) -> Unit,
    onEffort: (String) -> Unit,
    onCompact: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "对话设置")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuLabel("模型")
            DropdownMenuItem(
                text = { Text("deepseek-flash") },
                onClick = {
                    onModel(SettingsStore.MODEL_FLASH)
                    expanded = false
                },
                trailingIcon = {
                    if (model == SettingsStore.MODEL_FLASH) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                },
            )
            DropdownMenuItem(
                text = { Text("deepseek-v4-pro") },
                onClick = {
                    onModel(SettingsStore.MODEL_PRO)
                    expanded = false
                },
                trailingIcon = {
                    if (model == SettingsStore.MODEL_PRO) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                },
            )
            HorizontalDivider()
            MenuLabel("思考强度")
            listOf(
                SettingsStore.EFFORT_LOW,
                SettingsStore.EFFORT_HIGH,
                SettingsStore.EFFORT_MAX,
            ).forEach { value ->
                DropdownMenuItem(
                    text = { Text(effortLabel(value)) },
                    onClick = {
                        onEffort(value)
                        expanded = false
                    },
                    trailingIcon = {
                        if (effort == value) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("整理记忆") },
                onClick = {
                    expanded = false
                    onCompact()
                },
            )
        }
    }
}

@Composable
private fun MenuLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ErrorBanner(text: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    }
}

private fun greetingText(): String {
    val hour = LocalTime.now(ZoneId.of("Asia/Shanghai")).hour
    val prefix = when {
        hour in 5..11 -> "早上好"
        hour in 12..17 -> "下午好"
        hour in 18..22 -> "晚上好"
        else -> "夜深了"
    }
    return "$prefix，今天有什么计划？"
}

private fun modelLabel(model: String): String = when (model) {
    SettingsStore.MODEL_PRO -> "V4 Pro"
    else -> "Flash"
}

private fun effortLabel(effort: String): String = when (effort) {
    SettingsStore.EFFORT_LOW -> "low"
    SettingsStore.EFFORT_MAX -> "max"
    else -> "high"
}
