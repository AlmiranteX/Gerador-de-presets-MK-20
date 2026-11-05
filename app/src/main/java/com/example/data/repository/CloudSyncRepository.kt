package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.PresetEntity
import com.example.data.model.Mk20Preset
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

@IgnoreExtraProperties
data class UserQuota(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val dailyDate: String = "2026-01-01",
    val dailyCount: Int = 0,
    val proLifetime: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudPreset(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val song: String = "",
    val style: String = "",
    val bpm: Int = 120,
    val youtubeUrl: String = "",
    val details: String = "",
    val jsonContent: String = "{}",
    val favorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toPresetEntity(localId: Int = 0): PresetEntity {
        val createdMillis = (createdAt ?: Timestamp.now()).toDate().time
        return PresetEntity(
            id = localId,
            title = title,
            song = song.ifBlank { title },
            style = style,
            bpm = bpm,
            youtubeUrl = youtubeUrl,
            details = details,
            jsonContent = jsonContent,
            isFavorite = favorite,
            createdAt = createdMillis
        )
    }

    companion object {
        fun fromMk20Preset(userId: String, preset: Mk20Preset, favorite: Boolean = false): CloudPreset {
            return CloudPreset(
                userId = userId,
                title = preset.title.take(120).ifBlank { "Preset MK-20" },
                song = preset.title.take(120),
                style = preset.style.take(80),
                bpm = preset.bpm.coerceIn(20, 300),
                youtubeUrl = preset.youtubeLink.take(500),
                details = preset.details.take(2000),
                jsonContent = preset.toJsonString().take(50000),
                favorite = favorite,
                createdAt = null,
                updatedAt = null
            )
        }
    }
}

class CloudSyncRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun getCurrentUserIdOrNull(): String? = auth.currentUser?.uid

    /**
     * Sincroniza a cota diária (4/4) e o status PRO Vitalício com o Cloud Firestore.
     * Impede que a reinstalação do aplicativo zere o contador de uso diário.
     */
    suspend fun syncUserQuota(
        todayDate: String,
        localCount: Int,
        localPro: Boolean,
        forceOverrideCloud: Boolean = false,
        resetProOnly: Boolean = false
    ): Result<UserQuota> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val userEmail = (auth.currentUser?.email ?: "").take(254)
        val userName = (auth.currentUser?.displayName ?: "").take(120)

        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(
                    UserQuota::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserQuota(userId = uid)

                val effectivePro = if (forceOverrideCloud || resetProOnly) {
                    localPro
                } else {
                    existing.proLifetime || localPro
                }

                val effectiveCount = if (forceOverrideCloud) {
                    localCount.coerceIn(0, 100)
                } else if (existing.dailyDate == todayDate) {
                    maxOf(existing.dailyCount, localCount).coerceIn(0, 100)
                } else {
                    // Novo dia: zera a cota diária na nuvem (ou usa o contador local de hoje)
                    localCount.coerceIn(0, 100)
                }

                val needsUpdate = existing.dailyDate != todayDate ||
                    existing.dailyCount != effectiveCount ||
                    existing.proLifetime != effectivePro

                if (needsUpdate && existing.createdAt != null) {
                    val updatedModel = existing.copy(
                        userId = uid,
                        email = userEmail,
                        displayName = userName,
                        dailyDate = todayDate,
                        dailyCount = effectiveCount,
                        proLifetime = effectivePro,
                        updatedAt = null // Aciona @ServerTimestamp no update mantendo createdAt intacto
                    )
                    docRef.set(updatedModel).await()
                    Result.success(updatedModel.copy(updatedAt = Timestamp.now()))
                } else {
                    Result.success(
                        existing.copy(
                            dailyDate = todayDate,
                            dailyCount = effectiveCount,
                            proLifetime = effectivePro
                        )
                    )
                }
            } else {
                val newQuota = UserQuota(
                    userId = uid,
                    email = userEmail,
                    displayName = userName,
                    dailyDate = todayDate,
                    dailyCount = localCount.coerceIn(0, 100),
                    proLifetime = localPro,
                    createdAt = null,
                    updatedAt = null
                )
                docRef.set(newQuota).await()
                Result.success(newQuota.copy(createdAt = Timestamp.now(), updatedAt = Timestamp.now()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserQuota(targetUserId: String = requireUserId()): Result<UserQuota?> {
        val docRef = db.collection("users").document(targetUserId)
        return try {
            val snapshot = docRef.get().await()
            val quota = snapshot.toObject(
                UserQuota::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(quota)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserQuota(): Flow<UserQuota?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserQuota::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    /**
     * Salva ou atualiza um preset na biblioteca da nuvem do usuário (/users/{uid}/presets/{presetId}).
     */
    suspend fun saveCloudPreset(
        preset: Mk20Preset,
        favorite: Boolean = false,
        customPresetId: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val rawId = customPresetId ?: sanitizePresetDocId(preset.title)
        val docId = rawId.ifBlank { "preset_${UUID.randomUUID().toString().take(8)}" }
        val docRef = db.collection("users").document(uid).collection("presets").document(docId)

        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existing = existingSnap.toObject(
                    CloudPreset::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val existingCreatedAt = existing?.createdAt ?: Timestamp.now()
                val updated = CloudPreset.fromMk20Preset(uid, preset, favorite).copy(
                    id = docId,
                    createdAt = existingCreatedAt,
                    updatedAt = null
                )
                docRef.set(updated).await()
            } else {
                val created = CloudPreset.fromMk20Preset(uid, preset, favorite).copy(
                    id = docId,
                    createdAt = null,
                    updatedAt = null
                )
                docRef.set(created).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserPresets(): Result<List<CloudPreset>> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("presets")
        return try {
            val snapshot = colRef.whereEqualTo("userId", uid).get().await()
            val list = snapshot.toObjects(
                CloudPreset::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun getPresetById(presetId: String, ownerUserId: String = requireUserId()): Result<CloudPreset?> {
        val docRef = db.collection("users").document(ownerUserId).collection("presets").document(presetId)
        return try {
            val snapshot = docRef.get().await()
            val item = snapshot.toObject(
                CloudPreset::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserPresets(): Flow<List<CloudPreset>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/presets"
        emitAll(
            db.collection("users").document(uid).collection("presets")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudPreset::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun deleteCloudPreset(presetId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("presets").document(presetId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        fun sanitizePresetDocId(title: String): String {
            val clean = title.trim()
                .replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
                .replace("_{2,}".toRegex(), "_")
                .trim('_')
                .lowercase()
                .take(80)
            return clean.ifBlank { "preset_${UUID.randomUUID().toString().take(8)}" }
        }
    }
}
