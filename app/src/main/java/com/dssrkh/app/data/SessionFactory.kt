package com.dssrkh.app.data

import com.dssrkh.app.data.db.AppDatabase
import com.dssrkh.app.data.db.CharacterEntity
import com.dssrkh.app.data.db.MessageEntity
import com.dssrkh.app.data.db.SessionEntity

object SessionFactory {

    suspend fun create(app: AppDatabase, character: CharacterEntity): Long {
        val sessionId = app.sessionDao().insert(SessionEntity(characterId = character.id))
        character.openingMessage?.takeIf { it.isNotBlank() }?.let { opening ->
            app.messageDao().insert(
                MessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    rawText = opening,
                    displayText = opening,
                    status = "done",
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
        return sessionId
    }
}
