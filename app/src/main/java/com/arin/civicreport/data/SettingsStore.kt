package com.arin.civicreport.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arin.civicreport.model.AppSettings
import com.arin.civicreport.model.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "duarbagori_settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val channel = stringPreferencesKey("default_channel")
        val notifications = booleanPreferencesKey("notifications")
        val senderName = stringPreferencesKey("sender_name")
        val senderEmail = stringPreferencesKey("sender_email")
        val senderPhone = stringPreferencesKey("sender_phone")
        val highAccuracy = booleanPreferencesKey("high_accuracy")
        val national = booleanPreferencesKey("include_national")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            defaultChannel = p[Keys.channel]?.let { runCatching { SendChannel.valueOf(it) }.getOrNull() }
                ?: SendChannel.BOTH,
            notificationsEnabled = p[Keys.notifications] ?: true,
            senderName = p[Keys.senderName].orEmpty(),
            senderEmail = p[Keys.senderEmail].orEmpty(),
            senderPhone = p[Keys.senderPhone].orEmpty(),
            highAccuracy = p[Keys.highAccuracy] ?: true,
            includeNationalAuthorities = p[Keys.national] ?: true
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val current = AppSettings(
                defaultChannel = p[Keys.channel]?.let { runCatching { SendChannel.valueOf(it) }.getOrNull() }
                    ?: SendChannel.BOTH,
                notificationsEnabled = p[Keys.notifications] ?: true,
                senderName = p[Keys.senderName].orEmpty(),
                senderEmail = p[Keys.senderEmail].orEmpty(),
                senderPhone = p[Keys.senderPhone].orEmpty(),
                highAccuracy = p[Keys.highAccuracy] ?: true,
                includeNationalAuthorities = p[Keys.national] ?: true
            )
            val next = transform(current)
            p[Keys.channel] = next.defaultChannel.name
            p[Keys.notifications] = next.notificationsEnabled
            p[Keys.senderName] = next.senderName
            p[Keys.senderEmail] = next.senderEmail
            p[Keys.senderPhone] = next.senderPhone
            p[Keys.highAccuracy] = next.highAccuracy
            p[Keys.national] = next.includeNationalAuthorities
        }
    }
}
