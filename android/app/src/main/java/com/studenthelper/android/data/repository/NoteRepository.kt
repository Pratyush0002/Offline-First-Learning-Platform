package com.studenthelper.android.data.repository

import com.google.gson.Gson
import com.studenthelper.android.crdt.*
import com.studenthelper.android.data.local.dao.CrdtOpDao
import com.studenthelper.android.data.local.dao.NoteDao
import com.studenthelper.android.data.local.entity.CrdtOpEntity
import com.studenthelper.android.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    private val crdtOpDao: CrdtOpDao,
    private val clientId: String = "student-android-" + UUID.randomUUID().toString().substring(0, 6)
) {

    private val localClock = LamportClock(1)
    private val gson = Gson()

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()

    fun getNotesForLesson(lessonId: String): Flow<List<NoteEntity>> {
        return noteDao.getNotesByLesson(lessonId)
    }

    fun getNote(noteId: String): Flow<NoteEntity?> {
        return noteDao.getNoteById(noteId)
    }

    suspend fun createNote(title: String, lessonId: String?): String {
        val noteId = "note-" + UUID.randomUUID().toString().substring(0, 8)
        val clock = localClock.tick()

        val op = CrdtOperation.createNote(noteId, lessonId, title, clientId, clock)
        saveLocalOperation(op)

        val noteEntity = NoteEntity(
            id = noteId,
            lessonId = lessonId,
            authorId = clientId,
            title = title,
            titleClock = clock,
            titleClientId = clientId,
            contentMarkdown = "# $title\n\n",
            isDeleted = false,
            isSyncedWithServer = false
        )
        noteDao.insertOrUpdate(noteEntity)
        return noteId
    }

    suspend fun addBlock(noteId: String, content: String, type: BlockType = BlockType.PARAGRAPH): String {
        val blockId = "b-" + UUID.randomUUID().toString().substring(0, 6)
        val clock = localClock.tick()
        val pos = FractionalIndex.initial(clientId, clock)

        val op = CrdtOperation.insertBlock(noteId, blockId, pos, type, content, clientId, clock)
        saveLocalOperation(op)

        rebuildAndSaveNote(noteId)
        return blockId
    }

    suspend fun updateTitle(noteId: String, newTitle: String) {
        val clock = localClock.tick()
        val op = CrdtOperation.updateTitle(noteId, newTitle, clientId, clock)
        saveLocalOperation(op)
        rebuildAndSaveNote(noteId)
    }

    suspend fun addTag(noteId: String, tag: String) {
        val clock = localClock.tick()
        val op = CrdtOperation.addTag(noteId, tag, clientId, clock)
        saveLocalOperation(op)
        rebuildAndSaveNote(noteId)
    }

    suspend fun deleteNote(noteId: String) {
        val clock = localClock.tick()
        val op = CrdtOperation.deleteNote(noteId, clientId, clock)
        saveLocalOperation(op)
        noteDao.markDeleted(noteId, clock)
    }

    private suspend fun saveLocalOperation(op: CrdtOperation) {
        val entity = CrdtOpEntity(
            opId = op.opId,
            noteId = op.noteId,
            type = op.type.name,
            clientId = op.clientId,
            lamportClock = op.lamportClock,
            timestamp = op.timestamp,
            targetId = op.targetId,
            payload = op.payload,
            secondaryPayload = op.secondaryPayload,
            isSynced = false
        )
        crdtOpDao.insertOp(entity)
    }

    suspend fun rebuildAndSaveNote(noteId: String) {
        val opEntities = crdtOpDao.getOpsForNote(noteId)
        if (opEntities.isEmpty()) return

        val ops = opEntities.map { e ->
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
        }.sorted()

        var title = "Untitled Note"
        var lessonId: String? = null
        for (op in ops) {
            if (op.type == OperationType.CREATE_NOTE) {
                lessonId = op.targetId
                if (op.payload != null) title = op.payload
                break
            }
        }

        val crdtNote = CrdtNote(
            id = noteId,
            lessonId = lessonId,
            authorId = clientId,
            title = title,
            titleClock = 1L,
            titleClientId = clientId
        )
        CrdtEngine.applyBatch(crdtNote, ops)

        val existing = noteDao.getNoteDirect(noteId)
        val noteEntity = NoteEntity(
            id = noteId,
            lessonId = crdtNote.lessonId,
            authorId = crdtNote.authorId,
            title = crdtNote.title,
            titleClock = crdtNote.titleClock,
            titleClientId = crdtNote.titleClientId,
            contentMarkdown = crdtNote.toMarkdown(),
            isDeleted = crdtNote.isDeleted,
            deleteClock = crdtNote.deleteClock,
            tagsJson = gson.toJson(crdtNote.getActiveTags()),
            crdtStateJson = gson.toJson(crdtNote),
            isSyncedWithServer = existing?.isSyncedWithServer ?: false,
            updatedAt = crdtNote.updatedAt
        )
        noteDao.insertOrUpdate(noteEntity)
    }

    fun getClientId(): String = clientId
    fun getLocalClock(): Long = localClock.get()
}
