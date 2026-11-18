package com.dssrkh.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM `character` ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM `character` WHERE id = :id")
    suspend fun get(id: Long): CharacterEntity?

    @Insert
    suspend fun insert(item: CharacterEntity): Long

    @Update
    suspend fun update(item: CharacterEntity)

    @Query("DELETE FROM `character` WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM `session` WHERE characterId = :characterId ORDER BY pinned DESC, updatedAt DESC")
    fun observeByCharacter(characterId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM `session` WHERE id = :id")
    suspend fun get(id: Long): SessionEntity?

    @Query("SELECT * FROM `session` WHERE characterId = :characterId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latest(characterId: Long): SessionEntity?

    @Query("SELECT * FROM `session` ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latestOverall(): SessionEntity?

    @Insert
    suspend fun insert(item: SessionEntity): Long

    @Query("UPDATE `session` SET title = :title, updatedAt = :ts WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE `session` SET modelOverride = :model WHERE id = :id")
    suspend fun updateModel(id: Long, model: String?)

    @Query("UPDATE `session` SET effortOverride = :effort WHERE id = :id")
    suspend fun updateEffort(id: Long, effort: String?)

    @Query("UPDATE `session` SET memorySnapshot = :snapshot WHERE id = :id")
    suspend fun updateSnapshot(id: Long, snapshot: String?)

    @Query("UPDATE `session` SET pinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean)

    @Query("UPDATE `session` SET lastPromptTokens = :tokens WHERE id = :id")
    suspend fun updateLastPromptTokens(id: Long, tokens: Long)

    @Query("UPDATE `session` SET compactionEpoch = compactionEpoch + 1 WHERE id = :id")
    suspend fun incrementEpoch(id: Long)

    @Query("UPDATE `session` SET updatedAt = :ts WHERE id = :id")
    suspend fun touch(id: Long, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM `session` WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM message WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
    fun observeBySession(sessionId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM message WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
    suspend fun listBySession(sessionId: Long): List<MessageEntity>

    @Query("SELECT * FROM message WHERE id = :id")
    suspend fun get(id: Long): MessageEntity?

    @Insert
    suspend fun insert(item: MessageEntity): Long

    @Update
    suspend fun update(item: MessageEntity)

    @Query(
        "UPDATE message SET rawText = :raw, displayText = :display, reasoningText = :reasoning, " +
            "status = :status WHERE id = :id",
    )
    suspend fun updateStream(id: Long, raw: String, display: String, reasoning: String?, status: String)

    @Query("UPDATE message SET usageJson = :usage WHERE id = :id")
    suspend fun updateUsage(id: Long, usage: String?)

    @Query("UPDATE message SET memoryCount = :count WHERE id = :id")
    suspend fun updateMemoryCount(id: Long, count: Int)

    @Query("UPDATE message SET blocksJson = :blocks WHERE id = :id")
    suspend fun updateBlocks(id: Long, blocks: String?)

    @Query("DELETE FROM message WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM message WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("UPDATE message SET compacted = 1 WHERE sessionId = :sessionId")
    suspend fun markAllCompacted(sessionId: Long)
}

@Dao
interface SummaryDao {
    @Insert
    suspend fun insert(item: SummaryEntity): Long

    @Query("SELECT * FROM summary WHERE sessionId = :sessionId ORDER BY epoch DESC, id DESC LIMIT 1")
    suspend fun latest(sessionId: Long): SummaryEntity?

    @Query("DELETE FROM summary WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory WHERE characterId = :characterId ORDER BY pinned DESC, importance DESC, createdAt DESC")
    fun observeByCharacter(characterId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory WHERE characterId = :characterId AND status = 'active'")
    suspend fun activeFor(characterId: Long): List<MemoryEntity>

    @Query("SELECT * FROM memory WHERE id = :id")
    suspend fun get(id: Long): MemoryEntity?

    @Insert
    suspend fun insert(item: MemoryEntity): Long

    @Update
    suspend fun update(item: MemoryEntity)

    @Query("DELETE FROM memory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memory WHERE characterId = :characterId")
    suspend fun deleteByCharacter(characterId: Long)

    @Query("UPDATE memory SET status = 'revoked' WHERE sourceMessageId = :messageId")
    suspend fun revokeBySource(messageId: Long)
}

@Dao
interface UsageDao {
    @Insert
    suspend fun insert(item: UsageRecordEntity): Long

    @Query("SELECT * FROM usage_record ORDER BY ts DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<UsageRecordEntity>>
}
