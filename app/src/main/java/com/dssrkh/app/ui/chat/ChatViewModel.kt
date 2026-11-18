package com.dssrkh.app.ui.chat

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dssrkh.app.DssrkhApp
import com.dssrkh.app.data.AttachmentCodec
import com.dssrkh.app.data.FileStore
import com.dssrkh.app.data.ImageStore
import com.dssrkh.app.data.LocalAttachment
import com.dssrkh.app.data.SettingsStore
import com.dssrkh.app.data.files.FilesService
import com.dssrkh.app.data.api.ChatRequestDto
import com.dssrkh.app.data.api.StreamEvent
import com.dssrkh.app.data.api.ThinkingDto
import com.dssrkh.app.data.api.TitleDto
import com.dssrkh.app.data.api.ToolDto
import com.dssrkh.app.data.api.UsageDto
import com.dssrkh.app.data.asr.WavRecorder
import com.dssrkh.app.data.db.CharacterEntity
import com.dssrkh.app.data.db.MessageEntity
import com.dssrkh.app.data.db.SessionEntity
import com.dssrkh.app.data.db.UsageRecordEntity
import com.dssrkh.app.domain.CompactionService
import com.dssrkh.app.domain.CostCalculator
import com.dssrkh.app.domain.MemTagFilter
import com.dssrkh.app.domain.MemTagParser
import com.dssrkh.app.domain.MemoryService
import com.dssrkh.app.domain.PromptBuilder
import com.dssrkh.app.domain.TtsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File

data class ChatMessageUi(
    val id: Long,
    val role: String,
    val text: String,
    val reasoning: String? = null,
    val status: String = "done",
    val memoryCount: Int = 0,
    val streaming: Boolean = false,
    val attachments: List<LocalAttachment> = emptyList(),
    val blocksJson: String? = null,
    val importedOfficial: Boolean = false,
)

data class ChatUiState(
    val loading: Boolean = true,
    val character: CharacterEntity? = null,
    val session: SessionEntity? = null,
    val messages: List<ChatMessageUi> = emptyList(),
    val input: String = "",
    val streaming: Boolean = false,
    val error: String? = null,
    val model: String = SettingsStore.MODEL_FLASH,
    val effort: String = SettingsStore.EFFORT_HIGH,
    val pendingImages: List<LocalAttachment> = emptyList(),
    val pendingFiles: List<LocalAttachment> = emptyList(),
    val uploading: Boolean = false,
    val notice: String? = null,
    val speakingId: Long? = null,
    val recording: Boolean = false,
)

class ChatViewModel(application: Application, private val sessionId: Long) : AndroidViewModel(application) {

    private val root = application as DssrkhApp
    private val db = root.database
    private val settings = root.settings
    private val client = root.client
    private val memory = MemoryService(db)
    private val compaction = CompactionService(db, client, memory)
    private val json = Json { encodeDefaults = true }

    var state by mutableStateOf(ChatUiState())
        private set

    private var streamJob: Job? = null
    private var assistantId: Long = 0
    private var stoppedByUser = false
    private var failure: String? = null
    private var recorder: WavRecorder? = null
    private var recordTimeout: Job? = null

    init {
        load()
        observeMessages()
    }

    private fun load() {
        viewModelScope.launch {
            val session = db.sessionDao().get(sessionId) ?: return@launch
            val character = db.characterDao().get(session.characterId) ?: return@launch
            state = state.copy(
                loading = false,
                session = session,
                character = character,
                model = session.modelOverride ?: settings.defaultModel,
                effort = session.effortOverride ?: settings.defaultEffort,
            )
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            db.messageDao().observeBySession(sessionId).collect { list ->
                if (state.streaming) return@collect
                state = state.copy(messages = list.map { it.toUi() })
            }
        }
    }

    fun updateInput(value: String) {
        state = state.copy(input = value)
    }

    fun clearError() {
        state = state.copy(error = null)
    }

    fun refreshCharacter() {
        viewModelScope.launch {
            state.character?.let { current ->
                db.characterDao().get(current.id)?.let {
                    state = state.copy(character = it)
                }
            }
        }
    }

    fun clearNotice() {
        state = state.copy(notice = null)
    }

    fun showNotice(message: String) {
        state = state.copy(notice = message)
    }

    fun startRecording() {
        if (state.recording) return
        val newRecorder = WavRecorder(getApplication())
        if (!newRecorder.start()) {
            state = state.copy(notice = "无法开始录音")
            return
        }
        recorder = newRecorder
        state = state.copy(recording = true)
        recordTimeout = viewModelScope.launch {
            delay(110_000)
            stopRecording()
        }
    }

    fun stopRecording() {
        val active = recorder ?: return
        recorder = null
        recordTimeout?.cancel()
        recordTimeout = null
        state = state.copy(recording = false)
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { active.stop() }
            if (file == null) {
                state = state.copy(notice = "录音太短")
                return@launch
            }
            state = state.copy(notice = "正在识别…")
            root.asr.transcribe(file)
                .onSuccess { text ->
                    state = state.copy(input = (state.input + text).trim(), notice = null)
                }
                .onFailure { error ->
                    state = state.copy(notice = error.message ?: "语音识别失败")
                }
            file.delete()
        }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (state.model == SettingsStore.MODEL_PRO) {
            state = state.copy(notice = "deepseek-v4-pro 不支持图片，请切换到 deepseek-flash")
            return
        }
        val room = (9 - state.pendingImages.size).coerceAtLeast(0)
        if (room == 0) {
            state = state.copy(notice = "每条消息最多 9 张图片")
            return
        }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val saved = withContext(Dispatchers.IO) {
                uris.take(room).mapNotNull { ImageStore.saveImage(context, it) }
            }
            state = state.copy(pendingImages = (state.pendingImages + saved).take(9))
        }
    }

    fun removePendingImage(path: String) {
        state = state.copy(pendingImages = state.pendingImages.filterNot { it.path == path })
    }

    fun addDocument(uri: Uri) {
        if (state.model == SettingsStore.MODEL_PRO) {
            state = state.copy(notice = "deepseek-v4-pro 不支持文档，请切换到 deepseek-flash")
            return
        }
        if (state.pendingFiles.size >= 20) {
            state = state.copy(notice = "每个请求最多引用 20 个文档")
            return
        }
        viewModelScope.launch {
            val context = getApplication<Application>()
            FileStore.saveDocument(context, uri)?.let { attachment ->
                state = state.copy(pendingFiles = state.pendingFiles + attachment)
            }
        }
    }

    fun removePendingFile(path: String) {
        state = state.copy(pendingFiles = state.pendingFiles.filterNot { it.path == path })
    }

    fun speak(message: ChatMessageUi) {
        if (message.text.isBlank() || message.streaming) return
        if (state.speakingId == message.id) {
            root.player.stop()
            state = state.copy(speakingId = null)
            return
        }
        viewModelScope.launch {
            val voice = state.character?.voiceId?.takeIf { it.isNotBlank() } ?: "stella"
            root.tts.synthesize(TtsText.prepare(message.text), voice)
                .onSuccess { file ->
                    root.player.stop()
                    root.player.play(file)
                    state = state.copy(speakingId = message.id)
                }
                .onFailure { error ->
                    state = state.copy(speakingId = null, notice = error.message ?: "朗读失败")
                }
        }
    }

    override fun onCleared() {
        root.player.stop()
        recorder?.stop()
        super.onCleared()
    }

    fun setModel(model: String) {
        state = state.copy(model = model)
        viewModelScope.launch { db.sessionDao().updateModel(sessionId, model) }
    }

    fun setEffort(effort: String) {
        state = state.copy(effort = effort)
        viewModelScope.launch { db.sessionDao().updateEffort(sessionId, effort) }
    }

    fun stop() {
        if (!state.streaming) return
        stoppedByUser = true
        streamJob?.cancel()
    }

    fun send() {
        val text = state.input.trim()
        val images = state.pendingImages
        val files = state.pendingFiles
        if ((text.isEmpty() && images.isEmpty() && files.isEmpty()) || state.streaming || state.uploading) return
        if (state.model == SettingsStore.MODEL_PRO && (images.isNotEmpty() || files.isNotEmpty())) {
            state = state.copy(notice = "deepseek-v4-pro 不支持图片与文档，请切换到 deepseek-flash")
            return
        }
        val character = state.character ?: return
        state = state.copy(input = "", error = null)
        stoppedByUser = false
        failure = null

        streamJob = viewModelScope.launch {
            val uploadedFiles = if (files.isEmpty()) {
                emptyList()
            } else {
                state = state.copy(uploading = true, notice = "正在上传文档…")
                val result = files.map { file ->
                    if (file.fileId != null) {
                        file
                    } else {
                        val purpose = FilesService.purposeFor(file.name.orEmpty())
                        val uploaded = root.files.upload(File(file.path), file.mime, purpose)
                            .getOrElse { error ->
                                state = state.copy(
                                    uploading = false,
                                    notice = "文档上传失败：${error.message}",
                                )
                                return@launch
                            }
                        file.copy(fileId = uploaded.id, purpose = uploaded.purpose)
                    }
                }
                state = state.copy(uploading = false, notice = null)
                result
            }

            state = state.copy(pendingImages = emptyList(), pendingFiles = emptyList())
            maybeCompact(character)
            val now = System.currentTimeMillis()
            db.messageDao().insert(
                MessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    rawText = text,
                    displayText = text,
                    attachmentsJson = (images + uploadedFiles)
                        .takeIf { it.isNotEmpty() }
                        ?.let(AttachmentCodec::encode),
                    status = "done",
                    createdAt = now,
                ),
            )
            db.sessionDao().touch(sessionId)
            runGeneration(
                character = character,
                fallbackTitle = text.ifBlank {
                    files.firstOrNull()?.name?.take(20) ?: "图片对话"
                },
            )
        }
    }

    private suspend fun maybeCompact(character: CharacterEntity) {
        val session = db.sessionDao().get(sessionId) ?: return
        val threshold = settings.compactionThreshold
        if (threshold <= 0 || session.lastPromptTokens < threshold) return
        state = state.copy(notice = "上下文较长，正在整理记忆…")
        compaction.compact(session, character, state.model, settings.thinkingEnabled, state.effort)
            .onSuccess { state = state.copy(notice = "已整理记忆，继续对话") }
            .onFailure { state = state.copy(notice = "整理失败：${it.message}") }
        state = state.copy(session = db.sessionDao().get(sessionId))
    }

    fun compactNow() {
        if (state.streaming) return
        val character = state.character ?: return
        viewModelScope.launch {
            val session = db.sessionDao().get(sessionId) ?: return@launch
            state = state.copy(notice = "正在整理记忆…")
            compaction.compact(session, character, state.model, settings.thinkingEnabled, state.effort)
                .onSuccess { state = state.copy(notice = "已整理记忆") }
                .onFailure { state = state.copy(notice = "整理失败：${it.message}") }
            state = state.copy(session = db.sessionDao().get(sessionId))
        }
    }

    fun regenerate(messageId: Long) {
        if (state.streaming) return
        val target = state.messages.lastOrNull() ?: return
        if (target.id != messageId || target.role != "assistant") return
        val character = state.character ?: return
        val fallback = state.messages.lastOrNull { it.role == "user" }?.text.orEmpty()
        stoppedByUser = false
        failure = null

        streamJob = viewModelScope.launch {
            db.memoryDao().revokeBySource(target.id)
            db.messageDao().deleteById(target.id)
            state = state.copy(messages = state.messages.dropLast(1), error = null)
            runGeneration(character = character, fallbackTitle = fallback)
        }
    }

    fun deleteMessage(messageId: Long) {
        if (state.streaming) return
        viewModelScope.launch {
            db.memoryDao().revokeBySource(messageId)
            db.messageDao().deleteById(messageId)
            db.sessionDao().touch(sessionId)
        }
    }

    /** 编辑用户消息：截断其后内容并重新生成。 */
    fun editUserMessage(messageId: Long, newText: String) {
        if (state.streaming || newText.isBlank()) return
        val character = state.character ?: return
        stoppedByUser = false
        failure = null

        streamJob = viewModelScope.launch {
            val all = db.messageDao().listBySession(sessionId)
            val index = all.indexOfFirst { it.id == messageId && it.role == "user" }
            if (index < 0) return@launch
            val removed = all.drop(index + 1)
            removed.forEach { message ->
                db.memoryDao().revokeBySource(message.id)
                db.messageDao().deleteById(message.id)
            }
            db.messageDao().update(
                all[index].copy(rawText = newText.trim(), displayText = newText.trim()),
            )
            state = state.copy(
                messages = state.messages
                    .take(index + 1)
                    .map { if (it.id == messageId) it.copy(text = newText.trim()) else it },
                error = null,
            )
            runGeneration(character = character, fallbackTitle = newText.trim())
        }
    }

    private suspend fun runGeneration(character: CharacterEntity, fallbackTitle: String) {
        // 先置 streaming，避免消息表的 Flow 在占位消息插入后抢先回写造成重复 key。
        state = state.copy(streaming = true)
        assistantId = db.messageDao().insert(
            MessageEntity(
                sessionId = sessionId,
                role = "assistant",
                rawText = "",
                displayText = "",
                status = "streaming",
                createdAt = System.currentTimeMillis(),
                model = state.model,
            ),
        )

        val history = db.messageDao().listBySession(sessionId)
            .filter { it.id != assistantId && !it.compacted }
        val firstRequest = (state.session?.title ?: "").isBlank()
        val isFreshSession = history.count { it.role == "user" } <= 1
        val snapshot = if (isFreshSession && state.session?.memorySnapshot == null) {
            val built = memory.buildSnapshot(character.id)
            db.sessionDao().updateSnapshot(sessionId, built)
            state = state.copy(session = state.session?.copy(memorySnapshot = built))
            built
        } else {
            state.session?.memorySnapshot
        }

        val attachmentsByMessage = withContext(Dispatchers.IO) {
            history.mapNotNull { message ->
                val attachments = AttachmentCodec.decode(message.attachmentsJson)
                    .filter { it.kind == "image" || (it.kind == "file" && it.fileId != null) }
                if (attachments.isEmpty()) return@mapNotNull null
                val blocks = buildJsonArray {
                    attachments.forEach { attachment ->
                        when (attachment.kind) {
                            "image" -> AttachmentCodec.dataUrl(attachment)?.let { url ->
                                addJsonObject {
                                    put("type", "image_url")
                                    putJsonObject("image_url") { put("url", url) }
                                }
                            }
                            "file" -> attachment.fileId?.let { fileId ->
                                addJsonObject {
                                    put("type", "file")
                                    put("file_id", fileId)
                                }
                            }
                        }
                    }
                    val content = message.displayText.ifBlank { message.rawText }
                    if (content.isNotBlank()) {
                        addJsonObject {
                            put("type", "text")
                            put("text", content)
                        }
                    }
                }
                message.id to blocks
            }.toMap()
        }

        val summary = db.summaryDao().latest(sessionId)?.text
        val webEnabled = settings.webSearchEnabled &&
            state.model == SettingsStore.MODEL_FLASH &&
            character.allowSearch
        val tools: List<ToolDto>? = if (webEnabled) {
            buildList<ToolDto> {
                add(ToolDto("deepseek_web_search"))
                if (settings.readLinks) add(ToolDto("deepseek_read_url"))
            }
        } else {
            null
        }
        val request = ChatRequestDto(
            model = state.model,
            messages = PromptBuilder.buildMessages(
                character = character,
                history = history,
                userProfile = settings.profileText.takeIf { settings.shareProfile },
                memorySnapshot = snapshot,
                summary = summary,
                webSearchEnabled = webEnabled,
                contentBlocks = { message -> attachmentsByMessage[message.id] },
            ),
            thinking = ThinkingDto(if (settings.thinkingEnabled) "enabled" else "disabled"),
            reasoningEffort = if (settings.thinkingEnabled) state.effort else null,
            title = if (firstRequest) TitleDto() else null,
            tools = tools,
        )

        val content = StringBuilder()
        val reasoning = StringBuilder()
        val display = StringBuilder()
        val filter = MemTagFilter()
        var lastSave = 0L
        var title: String? = null
        var usage: UsageDto? = null
        var blocksJson: String? = null

        state = state.copy(
            streaming = true,
            messages = state.messages.filterNot { it.id == assistantId } + ChatMessageUi(
                id = assistantId,
                role = "assistant",
                text = "",
                status = "streaming",
                streaming = true,
            ),
        )

        try {
            client.streamChat(request).collect { event ->
                when (event) {
                    is StreamEvent.Content -> {
                        content.append(event.text)
                        display.append(filter.feed(event.text))
                    }
                    is StreamEvent.Reasoning -> reasoning.append(event.text)
                    is StreamEvent.Title -> title = event.text
                    is StreamEvent.Usage -> usage = event.usage
                    is StreamEvent.Blocks -> {
                        blocksJson = """{"annotations":${event.annotationsJson},"server_tool_calls":${event.toolCallsJson}}"""
                    }
                    is StreamEvent.Done -> Unit
                    is StreamEvent.Failed -> failure = event.message
                }
                val t = System.currentTimeMillis()
                if (t - lastSave > 400) {
                    lastSave = t
                    db.messageDao().updateStream(
                        assistantId,
                        content.toString(),
                        display.toString(),
                        reasoning.toString().ifEmpty { null },
                        "streaming",
                    )
                    updateStreamingUi(display.toString(), reasoning.toString())
                }
            }
            display.append(filter.finish())
        } catch (e: CancellationException) {
            throw e
        } finally {
            withContext(NonCancellable) {
                finalizeMessage(
                    raw = content.toString(),
                    liveDisplay = display.toString(),
                    reasoning = reasoning.toString(),
                    usage = usage,
                    title = title,
                    fallbackTitle = fallbackTitle,
                    blocksJson = blocksJson,
                )
            }
        }
    }

    private suspend fun finalizeMessage(
        raw: String,
        liveDisplay: String,
        reasoning: String,
        usage: UsageDto?,
        title: String?,
        fallbackTitle: String,
        blocksJson: String?,
    ) {
        val parsed = MemTagParser.parse(raw)
        val status = when {
            failure != null -> "error"
            stoppedByUser -> "interrupted"
            else -> "done"
        }
        val display = parsed.displayText.ifBlank { liveDisplay.trim() }
        val dao = db.messageDao()
        dao.updateStream(assistantId, raw, display, reasoning.ifBlank { null }, status)
        dao.updateUsage(assistantId, usage?.let { json.encodeToString(UsageDto.serializer(), it) })
        dao.updateBlocks(assistantId, blocksJson)

        var savedMemories = 0
        if (status == "done" && parsed.drafts.isNotEmpty()) {
            savedMemories = memory.persistDrafts(
                characterId = state.character?.id ?: 0L,
                messageId = assistantId,
                drafts = parsed.drafts,
            )
            if (savedMemories > 0) dao.updateMemoryCount(assistantId, savedMemories)
        }

        val currentTitle = state.session?.title.orEmpty()
        if (currentTitle.isBlank() && assistantId != 0L) {
            val newTitle = title?.takeIf { it.isNotBlank() }
                ?: fallbackTitle.replace('\n', ' ').take(20).trim()
            if (newTitle.isNotBlank()) {
                db.sessionDao().updateTitle(sessionId, newTitle)
                state = state.copy(session = state.session?.copy(title = newTitle))
            }
        }

        usage?.let {
            recordUsage(it)
            db.sessionDao().updateLastPromptTokens(sessionId, it.promptTokens)
        }

        state = state.copy(
            streaming = false,
            error = failure,
            messages = state.messages.map {
                if (it.id == assistantId) {
                    it.copy(
                        text = display,
                        reasoning = reasoning.ifBlank { null },
                        status = status,
                        memoryCount = savedMemories,
                        streaming = false,
                        blocksJson = blocksJson,
                    )
                } else {
                    it
                }
            },
        )
        if (failure == null) db.sessionDao().touch(sessionId)
    }

    private suspend fun recordUsage(usage: UsageDto) {
        val peak = CostCalculator.isPeak(System.currentTimeMillis())
        val cost = CostCalculator.costCny(
            model = state.model,
            peak = peak,
            hit = usage.promptCacheHitTokens,
            miss = usage.promptCacheMissTokens,
            output = usage.completionTokens,
        )
        db.usageDao().insert(
            UsageRecordEntity(
                sessionId = sessionId,
                characterId = state.character?.id,
                kind = "chat",
                model = state.model,
                inputHit = usage.promptCacheHitTokens,
                inputMiss = usage.promptCacheMissTokens,
                output = usage.completionTokens,
                reasoningTokens = usage.completionDetails?.reasoningTokens ?: 0,
                searchCount = usage.serverToolUse?.toolSearch ?: 0,
                estCostCny = cost,
                peak = peak,
            ),
        )
    }

    private fun updateStreamingUi(text: String, reasoning: String) {
        state = state.copy(
            messages = state.messages.map {
                if (it.id == assistantId) {
                    it.copy(text = text, reasoning = reasoning.ifEmpty { null })
                } else {
                    it
                }
            },
        )
    }

    private fun MessageEntity.toUi(): ChatMessageUi = ChatMessageUi(
        id = id,
        role = role,
        text = displayText.ifBlank { rawText },
        reasoning = reasoningText,
        status = status,
        memoryCount = memoryCount,
        streaming = status == "streaming",
        attachments = AttachmentCodec.decode(attachmentsJson),
        blocksJson = blocksJson,
        importedOfficial = blocksJson?.contains("\"importedOfficial\":true") == true,
    )
}
