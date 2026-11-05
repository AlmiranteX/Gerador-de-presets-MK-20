package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Mk20Preset
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncUserQuota_authenticatedOwner_persistsAndPreventsReinstallReset() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)

        // First sync: user generated 3 presets today
        val firstSync = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncUserQuota(
                todayDate = "2026-10-30",
                localCount = 3,
                localPro = false,
                forceOverrideCloud = true
            )
        }
        assertTrue(firstSync.isSuccess)
        assertEquals(3, firstSync.getOrThrow().dailyCount)

        // Simulate app reinstall (localCount = 0 on fresh install)
        val reinstallSync = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncUserQuota(
                todayDate = "2026-10-30",
                localCount = 0,
                localPro = false,
                forceOverrideCloud = false
            )
        }
        assertTrue(reinstallSync.isSuccess)
        // Cloud count (3) must be preserved so reinstall does not reset quota to 0!
        assertEquals(3, reinstallSync.getOrThrow().dailyCount)
    }

    @Test
    fun saveAndGetUserPresets_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)
        val dynamicId = "preset_${UUID.randomUUID().toString().take(8)}"
        val samplePreset = Mk20Preset.DEFAULT_PRESET.copy(title = "Solo Master $dynamicId")

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveCloudPreset(samplePreset, favorite = true, customPresetId = dynamicId)
        }
        assertTrue(saveResult.isSuccess)

        val presetsResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserPresets()
        }
        assertTrue(presetsResult.isSuccess)
        assertTrue(presetsResult.getOrThrow().any { it.id == dynamicId })
    }

    @Test
    fun observeUserPresets_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)
        val dynamicId = "preset_${UUID.randomUUID().toString().take(8)}"
        val samplePreset = Mk20Preset.DEFAULT_PRESET.copy(title = "Live Preset $dynamicId")

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveCloudPreset(samplePreset, favorite = false, customPresetId = dynamicId).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserPresets().first { list -> list.any { it.id == dynamicId } }
        }
        assertTrue(emitted.any { it.id == dynamicId })
    }

    @Test
    fun getPresetById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudSyncRepository(firestore)
        val dynamicId = "preset_${UUID.randomUUID().toString().take(8)}"
        val samplePreset = Mk20Preset.DEFAULT_PRESET.copy(title = "Alice Private Tone")

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveCloudPreset(samplePreset, favorite = true, customPresetId = dynamicId).getOrThrow()
        }

        // Bob signs in and attempts to read Alice's preset
        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudSyncRepository(firestore)
        val bobReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getPresetById(presetId = dynamicId, ownerUserId = aliceUid)
        }
        assertTrue(bobReadResult.isFailure)
        val firestoreEx = bobReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun getUserQuota_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncUserQuota("2026-10-30", 1, false).getOrThrow()
        }

        // Sign out to test unauthenticated rejection
        auth.signOut()
        val unauthResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserQuota(targetUserId = aliceUid)
        }
        assertTrue(unauthResult.isFailure)
        val firestoreEx = unauthResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
