package com.dssrkh.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 本地设置。API Key 单独存进 Keystore 加密的 EncryptedSharedPreferences，
 * 其余普通设置放普通 SharedPreferences。
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var apiKey: String
        get() = runCatching { securePrefs.getString("api_key", "") ?: "" }.getOrDefault("")
        set(value) {
            runCatching { securePrefs.edit().putString("api_key", value).apply() }
        }

    var defaultModel: String
        get() = prefs.getString("default_model", MODEL_FLASH) ?: MODEL_FLASH
        set(value) = prefs.edit().putString("default_model", value).apply()

    var defaultEffort: String
        get() = prefs.getString("default_effort", EFFORT_HIGH) ?: EFFORT_HIGH
        set(value) = prefs.edit().putString("default_effort", value).apply()

    var thinkingEnabled: Boolean
        get() = prefs.getBoolean("thinking_enabled", true)
        set(value) = prefs.edit().putBoolean("thinking_enabled", value).apply()

    var showReasoning: Boolean
        get() = prefs.getBoolean("show_reasoning", true)
        set(value) = prefs.edit().putBoolean("show_reasoning", value).apply()

    /** 跨角色共享的用户档案，默认关闭。 */
    var shareProfile: Boolean
        get() = prefs.getBoolean("share_profile", false)
        set(value) = prefs.edit().putBoolean("share_profile", value).apply()

    var profileText: String
        get() = prefs.getString("profile_text", "") ?: ""
        set(value) = prefs.edit().putString("profile_text", value).apply()

    var ageConfirmed: Boolean
        get() = prefs.getBoolean("age_confirmed", false)
        set(value) = prefs.edit().putBoolean("age_confirmed", value).apply()

    var qqGroupPrompted: Boolean
        get() = prefs.getBoolean("qq_group_prompted", false)
        set(value) = prefs.edit().putBoolean("qq_group_prompted", value).apply()

    /** 连续使用提醒，单位分钟；0 表示关闭。 */
    var reminderMinutes: Int
        get() = prefs.getInt("reminder_minutes", 120)
        set(value) = prefs.edit().putInt("reminder_minutes", value).apply()

    /** 压缩阈值（估算输入 tokens）。 */
    var compactionThreshold: Long
        get() = prefs.getLong("compaction_threshold", 800_000L)
        set(value) = prefs.edit().putLong("compaction_threshold", value).apply()

    var webSearchEnabled: Boolean
        get() = prefs.getBoolean("web_search", false)
        set(value) = prefs.edit().putBoolean("web_search", value).apply()

    var readLinks: Boolean
        get() = prefs.getBoolean("read_links", false)
        set(value) = prefs.edit().putBoolean("read_links", value).apply()

    companion object {
        const val MODEL_FLASH = "deepseek-flash"
        const val MODEL_PRO = "deepseek-v4-pro"
        const val EFFORT_LOW = "low"
        const val EFFORT_HIGH = "high"
        const val EFFORT_MAX = "max"
    }
}
