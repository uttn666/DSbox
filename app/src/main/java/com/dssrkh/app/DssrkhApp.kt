package com.dssrkh.app

import android.app.Application
import com.dssrkh.app.data.SettingsStore
import com.dssrkh.app.data.api.DeepSeekClient
import com.dssrkh.app.data.asr.AsrService
import com.dssrkh.app.data.db.AppDatabase
import com.dssrkh.app.data.files.FilesService
import com.dssrkh.app.data.tts.TtsService
import com.dssrkh.app.data.tts.VoicePlayer

class DssrkhApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val client: DeepSeekClient by lazy { DeepSeekClient(settings) }
    val tts: TtsService by lazy { TtsService(this, settings) }
    val player: VoicePlayer by lazy { VoicePlayer(this) }
    val asr: AsrService by lazy { AsrService(settings) }
    val files: FilesService by lazy { FilesService(settings) }
}
