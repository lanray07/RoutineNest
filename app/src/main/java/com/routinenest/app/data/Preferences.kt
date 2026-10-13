package com.routinenest.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("nest_preferences")
data class NestPreferences(val onboarded: Boolean = false, val theme: String = "system", val language: String = "en-GB", val speak: Boolean = false)
@Singleton
class PreferenceStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val onboarding = booleanPreferencesKey("onboarded")
    private val theme = stringPreferencesKey("theme")
    private val language = stringPreferencesKey("language")
    private val speak = booleanPreferencesKey("speak")
    val flow = context.dataStore.data.map { NestPreferences(it[onboarding] ?: false, it[theme] ?: "system", it[language] ?: "en-GB", it[speak] ?: false) }
    suspend fun onboardingDone() { context.dataStore.edit { it[onboarding] = true } }
    suspend fun theme(value: String) { require(value in setOf("system", "light", "dark")); context.dataStore.edit { it[theme] = value } }
    suspend fun language(value: String) { require(validatedLocales.any { it.first == value }); context.dataStore.edit { it[language] = value } }
    suspend fun speak(value: Boolean) { context.dataStore.edit { it[speak] = value } }
}
