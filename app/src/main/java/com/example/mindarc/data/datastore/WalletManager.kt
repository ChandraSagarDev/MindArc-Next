package com.example.mindarc.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallet")

@Singleton
class WalletManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val userEarnedCreditsKey = longPreferencesKey("user_earned_credits")

    suspend fun getUserEarnedCredits(): Long {
        val preferences = context.dataStore.data.first()
        return preferences[userEarnedCreditsKey] ?: 0
    }

    suspend fun deductCredits(amount: Long): Boolean {
        val currentCredits = getUserEarnedCredits()
        if (currentCredits >= amount) {
            context.dataStore.edit {
                it[userEarnedCreditsKey] = currentCredits - amount
            }
            return true
        }
        return false
    }
}
