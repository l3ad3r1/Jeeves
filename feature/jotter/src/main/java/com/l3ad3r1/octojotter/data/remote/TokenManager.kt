package com.l3ad3r1.octojotter.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TokenManager(context: Context) {
    private val masterKey by lazy {
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "secure_gist_notes_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _tokenFlow = MutableStateFlow("")
    val tokenFlow: StateFlow<String> = _tokenFlow

    init {
        // Keystore access during initialization can corrupt Robolectric's SQLite shadow
        if (!android.os.Build.FINGERPRINT.contains("robolectric")) {
            try {
                _tokenFlow.value = getToken() ?: ""
            } catch (e: Exception) {
                // Ignore for preview environments
            }
        }
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_GITHUB_TOKEN, token).apply()
        _tokenFlow.value = token
    }

    fun getToken(): String? {
        val raw = sharedPreferences.getString(KEY_GITHUB_TOKEN, null)
        return if (raw.isNullOrEmpty()) null else raw
    }

    fun clearToken() {
        sharedPreferences.edit().remove(KEY_GITHUB_TOKEN).apply()
        _tokenFlow.value = ""
    }

    companion object {
        private const val KEY_GITHUB_TOKEN = "github_pat_token"
    }
}
