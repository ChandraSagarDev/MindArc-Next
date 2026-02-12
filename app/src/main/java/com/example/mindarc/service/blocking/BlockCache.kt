package com.example.mindarc.service.blocking

import android.content.SharedPreferences
import com.example.mindarc.data.dao.RestrictedAppDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockCache @Inject constructor(
    private val restrictedAppDao: RestrictedAppDao,
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    private val ioDispatcher: CoroutineDispatcher
) {

    @Volatile
    private var blockedPackages: Set<String> = emptySet()

    companion object {
        private const val PREF_KEY_BLOCKED_PACKAGES = "blocked_packages_set"
    }

    init {
        loadFromPrefs()
    }

    fun shouldBlock(packageName: String): Boolean {
        return blockedPackages.contains(packageName)
    }

    suspend fun rebuildCache() = withContext(ioDispatcher) {
        val restrictedApps = restrictedAppDao.getBlockedApps().first()
        
        val newBlockedSet = restrictedApps
            .map { it.packageName }
            .toSet()

        blockedPackages = newBlockedSet
        saveToPrefs(newBlockedSet)
    }

    private fun loadFromPrefs() {
        val json = sharedPreferences.getString(PREF_KEY_BLOCKED_PACKAGES, null)
        if (json != null) {
            try {
                val type = object : TypeToken<Set<String>>() {}.type
                blockedPackages = gson.fromJson(json, type) ?: emptySet()
            } catch (e: Exception) {
                blockedPackages = emptySet()
            }
        }
    }
    
    private suspend fun saveToPrefs(set: Set<String>) = withContext(ioDispatcher) {
        val json = gson.toJson(set)
        sharedPreferences.edit().putString(PREF_KEY_BLOCKED_PACKAGES, json).apply()
    }
}
