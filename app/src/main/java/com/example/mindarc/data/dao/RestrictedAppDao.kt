package com.example.mindarc.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mindarc.data.model.RestrictedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface RestrictedAppDao {
    @Query("SELECT * FROM restricted_apps")
    fun getAllApps(): Flow<List<RestrictedApp>>

    @Query("SELECT * FROM restricted_apps WHERE isBlocked = 1")
    fun getBlockedApps(): Flow<List<RestrictedApp>>

    @Query("SELECT * FROM restricted_apps WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): RestrictedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: RestrictedApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<RestrictedApp>)

    @Update
    suspend fun updateApp(app: RestrictedApp)

    @Delete
    suspend fun deleteApp(app: RestrictedApp)

    @Query("DELETE FROM restricted_apps WHERE packageName = :packageName")
    suspend fun deleteAppByPackageName(packageName: String)
}

