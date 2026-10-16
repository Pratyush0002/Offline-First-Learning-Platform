package com.studenthelper.android.data.repository

import com.studenthelper.android.crdt.CrdtOperation
import com.studenthelper.android.crdt.OperationType
import com.studenthelper.android.crdt.VersionVector
import com.studenthelper.android.data.local.dao.CrdtOpDao
import com.studenthelper.android.data.local.dao.VectorClockDao
import com.studenthelper.android.data.local.entity.CrdtOpEntity
import com.studenthelper.android.data.remote.LearningApiService
import com.studenthelper.android.data.remote.dto.SyncRequestDto
import com.studenthelper.android.data.remote.dto.SyncResponseDto
import kotlinx.coroutines.flow.Flow

class SyncRepository(
    private val crdtOpDao: CrdtOpDao,
    private val vectorClockDao: VectorClockDao,
    private val noteRepository: NoteRepository,
    private val apiService: LearningApiService,
    private val clientId: String
) {

    val unsyncedCount: Flow<Int> = crdtOpDao.getUnsyncedCount()

    suspend fun performDeltaSync(): Result<SyncResponseDto> {
        return try {
            // 1. Fetch pending offline operations
            val pendingEntities = crdtOpDao.getPendingUnsyncedOps()
            val operationsToSend = pendingEntities.map { e ->
                CrdtOperation(
                    opId = e.opId,
                    noteId = e.noteId,
                    type = OperationType.valueOf(e.type),
                    clientId = e.clientId,
                    lamportClock = e.lamportClock,
                    timestamp = e.timestamp,
                    targetId = e.targetId,
                    payload = e.payload,
                    secondaryPayload = e.secondaryPayload
                )
            }

            // 2. Build local version vector
            val localClocks = vectorClockDao.getAllClocks()
            val versionVector = VersionVector()
            for (c in localClocks) {
                versionVector.updateMax(c.clientId, c.highestClock)
            }

            // 3. Send Sync Request to Spring Boot backend
            val request = SyncRequestDto(
                clientId = clientId,
                deviceName = "Android Student Device (${android.os.Build.MODEL})",
                clientVector = versionVector,
                operations = operationsToSend
            )

            val response = apiService.syncDelta(request)

            // 4. Mark acknowledged operations as synced in local SQLite
            if (response.acknowledgedOpIds.isNotEmpty()) {
                crdtOpDao.markOpsSynced(response.acknowledgedOpIds)
            }

            // 5. Ingest missing operations from other devices sent down by the server
            val affectedNotes = mutableSetOf<String>()
            for (remoteOp in response.missingOperations) {
                val entity = CrdtOpEntity(
                    opId = remoteOp.opId,
                    noteId = remoteOp.noteId,
                    type = remoteOp.type.name,
                    clientId = remoteOp.clientId,
                    lamportClock = remoteOp.lamportClock,
                    timestamp = remoteOp.timestamp,
                    targetId = remoteOp.targetId,
                    payload = remoteOp.payload,
                    secondaryPayload = remoteOp.secondaryPayload,
                    isSynced = true
                )
                crdtOpDao.insertOp(entity)
                vectorClockDao.updateMaxClock(remoteOp.clientId, remoteOp.lamportClock)
                affectedNotes.add(remoteOp.noteId)
            }

            // 6. Rebuild affected notes so they converge locally without conflicts
            for (noteId in affectedNotes) {
                noteRepository.rebuildAndSaveNote(noteId)
            }

            // 7. Update server's vector clock into local clock store
            for ((remoteClient, remoteClock) in response.serverVector.clocks) {
                vectorClockDao.updateMaxClock(remoteClient, remoteClock)
            }

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
