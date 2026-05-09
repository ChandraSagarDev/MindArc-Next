package com.example.mindarc.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteUserState(
    val totalPoints: Long,
    val activeUnlockEndTimeMillis: Long?
)

@Singleton
class FirebaseUserStore @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private fun uidOrNull(): String? = auth.currentUser?.uid

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    suspend fun ensureUserDocExists(): Boolean {
        val uid = uidOrNull() ?: return false
        val ref = userDoc(uid)
        val snap = ref.get().await()
        if (snap.exists()) return true
        ref.set(
            mapOf(
                "totalPoints" to 0L,
                "activeUnlockEndTime" to null,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return true
    }

    suspend fun getRemoteState(): RemoteUserState? {
        val uid = uidOrNull() ?: return null
        val snap = userDoc(uid).get().await()
        if (!snap.exists()) return RemoteUserState(totalPoints = 0L, activeUnlockEndTimeMillis = null)
        val points = (snap.getLong("totalPoints") ?: 0L).coerceAtLeast(0L)
        val unlockTs = snap.getTimestamp("activeUnlockEndTime")
        return RemoteUserState(
            totalPoints = points,
            activeUnlockEndTimeMillis = unlockTs?.toDate()?.time
        )
    }

    suspend fun setRemoteTotalPoints(totalPoints: Long) {
        val uid = uidOrNull() ?: return
        userDoc(uid).set(
            mapOf(
                "totalPoints" to totalPoints.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun incrementPoints(delta: Int) {
        if (delta == 0) return
        val uid = uidOrNull() ?: return
        userDoc(uid).update(
            mapOf(
                "totalPoints" to FieldValue.increment(delta.toLong()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Atomic spend, returns false if remote balance insufficient.
     * If successful, also writes `activeUnlockEndTime` so the extension can honor unlocks.
     */
    suspend fun spendPointsAndSetUnlock(pointsToSpend: Int, durationMinutes: Int): Boolean {
        if (pointsToSpend <= 0) return false
        val uid = uidOrNull() ?: return false
        val ref = userDoc(uid)

        return firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val current = (snap.getLong("totalPoints") ?: 0L).coerceAtLeast(0L)
            if (current < pointsToSpend.toLong()) return@runTransaction false
            val newPoints = current - pointsToSpend.toLong()
            val unlockUntilMillis = Timestamp.now().toDate().time + durationMinutes * 60_000L
            val unlockUntil = Timestamp(Date(unlockUntilMillis))
            tx.set(
                ref,
                mapOf(
                    "totalPoints" to newPoints,
                    "activeUnlockEndTime" to unlockUntil,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            true
        }.await()
    }
}

