package com.dssrkh.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "character")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarPath: String? = null,
    val summary: String? = null,
    val personaText: String = "",
    val openingMessage: String? = null,
    val userPersonaText: String? = null,
    val voiceId: String? = null,
    val allowSearch: Boolean = true,
    val bgOverride: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "session",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("characterId")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long,
    val title: String = "",
    val modelOverride: String? = null,
    val effortOverride: String? = null,
    val pinned: Boolean = false,
    val compactionEpoch: Int = 0,
    val memorySnapshot: String? = null,
    val lastPromptTokens: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "message",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("createdAt")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String,
    val rawText: String = "",
    val displayText: String = "",
    val reasoningText: String? = null,
    val blocksJson: String? = null,
    val attachmentsJson: String? = null,
    val usageJson: String? = null,
    val model: String? = null,
    val status: String = "done",
    val memoryCount: Int = 0,
    val epoch: Int = 0,
    val compacted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "memory",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("characterId"), Index("sourceMessageId")],
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long,
    val kind: String = "fact",
    val content: String,
    val importance: Int = 3,
    val emotionIntensity: Int = 0,
    val emotionTags: String? = null,
    val status: String = "active",
    val sourceMessageId: Long? = null,
    val pinned: Boolean = false,
    val recallCount: Int = 0,
    val lastRecalledAt: Long? = null,
    val supersededBy: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "summary",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val epoch: Int = 0,
    val text: String,
    val tokenEstimate: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "usage_record", indices = [Index("sessionId"), Index("ts")])
data class UsageRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long = System.currentTimeMillis(),
    val sessionId: Long? = null,
    val characterId: Long? = null,
    val kind: String = "chat",
    val model: String? = null,
    val inputHit: Long = 0,
    val inputMiss: Long = 0,
    val output: Long = 0,
    val reasoningTokens: Long = 0,
    val searchCount: Int = 0,
    val pages: Int = 0,
    val chars: Long = 0,
    val seconds: Long = 0,
    val estCostCny: Double = 0.0,
    val peak: Boolean = false,
)
