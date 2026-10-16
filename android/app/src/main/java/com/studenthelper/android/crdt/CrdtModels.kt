package com.studenthelper.android.crdt

import java.io.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class CrdtBlock(
    val blockId: String,
    var position: FractionalIndex,
    var type: BlockType = BlockType.PARAGRAPH,
    var content: String = "",
    var isChecked: Boolean = false,
    var isDeleted: Boolean = false,
    var lamportClock: Long = 0L,
    var clientId: String = "default",
    var lastModified: Long = System.currentTimeMillis()
) : Comparable<CrdtBlock>, Serializable {

    override fun compareTo(other: CrdtBlock): Int {
        val posCmp = this.position.compareTo(other.position)
        if (posCmp != 0) return posCmp
        return this.blockId.compareTo(other.blockId)
    }
}

data class CrdtOperation(
    val opId: String = UUID.randomUUID().toString(),
    val noteId: String,
    val type: OperationType,
    val clientId: String,
    val lamportClock: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val targetId: String? = null,
    val payload: String? = null,
    val secondaryPayload: String? = null
) : Comparable<CrdtOperation>, Serializable {

    override fun compareTo(other: CrdtOperation): Int {
        val clockCmp = this.lamportClock.compareTo(other.lamportClock)
        if (clockCmp != 0) return clockCmp
        val clientCmp = this.clientId.compareTo(other.clientId)
        if (clientCmp != 0) return clientCmp
        return this.opId.compareTo(other.opId)
    }

    companion object {
        fun createNote(noteId: String, lessonId: String?, title: String, clientId: String, clock: Long): CrdtOperation {
            return CrdtOperation(
                noteId = noteId,
                type = OperationType.CREATE_NOTE,
                clientId = clientId,
                lamportClock = clock,
                targetId = lessonId,
                payload = title
            )
        }

        fun updateTitle(noteId: String, title: String, clientId: String, clock: Long): CrdtOperation {
            return CrdtOperation(
                noteId = noteId,
                type = OperationType.UPDATE_TITLE,
                clientId = clientId,
                lamportClock = clock,
                payload = title
            )
        }

        fun insertBlock(noteId: String, blockId: String, pos: FractionalIndex, type: BlockType, content: String, clientId: String, clock: Long): CrdtOperation {
            return CrdtOperation(
                noteId = noteId,
                type = OperationType.INSERT_BLOCK,
                clientId = clientId,
                lamportClock = clock,
                targetId = blockId,
                payload = pos.position,
                secondaryPayload = "$content:::${type.name}"
            )
        }

        fun updateBlockContent(noteId: String, blockId: String, content: String, clientId: String, clock: Long): CrdtOperation {
            return CrdtOperation(
                noteId = noteId,
                type = OperationType.UPDATE_BLOCK_CONTENT,
                clientId = clientId,
                lamportClock = clock,
                targetId = blockId,
                payload = content
            )
        }

        fun toggleBlockCheck(noteId: String, blockId: String, checked: Boolean, clientId: String, clock: Long): CrdtOperation {
            return CrdtOperation(
                noteId = noteId,
                type = OperationType.TOGGLE_BLOCK_CHECK,
                clientId = clientId,
                lamportClock = clock,
                targetId = blockId,
                payload = checked.toString()
            )
        }

        fun deleteBlock(noteId: String, blockId: String, clientId: String, clock: Long): CrdtOperation {
            return CrdtOperation(
                noteId = noteId,
                type = OperationType.DELETE_BLOCK,
                clientId = clientId,
                lamportClock = clock,
                targetId = blockId
            )
        }

        fun addTag(noteId: String, tag: String, clientId: String, clock: Long): CrdtOperation {
            return CrdtOperation(
                noteId = noteId,
                type = OperationType.ADD_TAG,
                clientId = clientId,
                lamportClock = clock,
                targetId = tag
            )
        }

        fun removeTag(noteId: String, tag: String, clientId: String, clock: Long): CrdtOperation {
            return CrdtOperation(
                noteId = noteId,
                type = OperationType.REMOVE_TAG,
                clientId = clientId,
                lamportClock = clock,
                targetId = tag
            )
        }

        fun deleteNote(noteId: String, clientId: String, clock: Long): CrdtOperation {
            return CrdtOperation(
                noteId = noteId,
                type = OperationType.DELETE_NOTE,
                clientId = clientId,
                lamportClock = clock
            )
        }
    }
}

data class TagRecord(var addedClock: Long = 0L, var removedClock: Long = 0L) : Serializable {
    val isActive: Boolean get() = addedClock > removedClock
}

data class CrdtNote(
    val id: String,
    var lessonId: String?,
    val authorId: String,
    var title: String,
    var titleClock: Long,
    var titleClientId: String,
    val blocks: MutableMap<String, CrdtBlock> = ConcurrentHashMap(),
    val tags: MutableMap<String, TagRecord> = ConcurrentHashMap(),
    var isDeleted: Boolean = false,
    var deleteClock: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val versionVector: VersionVector = VersionVector()
) : Serializable {

    fun applyTitleUpdate(newTitle: String, clientId: String, clock: Long): Boolean {
        if (clock > titleClock || (clock == titleClock && clientId > titleClientId)) {
            title = newTitle
            titleClock = clock
            titleClientId = clientId
            updatedAt = System.currentTimeMillis()
            versionVector.updateMax(clientId, clock)
            return true
        }
        return false
    }

    fun getActiveBlocks(): List<CrdtBlock> {
        return blocks.values.filter { !it.isDeleted }.sorted()
    }

    fun getActiveTags(): Set<String> {
        return tags.filter { it.value.isActive }.keys.toSortedSet()
    }

    fun addTag(tag: String, clock: Long, clientId: String) {
        val record = tags.computeIfAbsent(tag) { TagRecord() }
        if (clock > record.addedClock) {
            record.addedClock = clock
            updatedAt = System.currentTimeMillis()
            versionVector.updateMax(clientId, clock)
        }
    }

    fun removeTag(tag: String, clock: Long, clientId: String) {
        val record = tags.computeIfAbsent(tag) { TagRecord() }
        if (clock > record.removedClock) {
            record.removedClock = clock
            updatedAt = System.currentTimeMillis()
            versionVector.updateMax(clientId, clock)
        }
    }

    fun markDeleted(clock: Long, clientId: String) {
        if (clock > deleteClock) {
            isDeleted = true
            deleteClock = clock
            updatedAt = System.currentTimeMillis()
            versionVector.updateMax(clientId, clock)
        }
    }

    fun toMarkdown(): String {
        val sb = StringBuilder()
        sb.append("# ").append(title).append("\n\n")

        val activeTags = getActiveTags()
        if (activeTags.isNotEmpty()) {
            sb.append("**Tags:** ").append(activeTags.joinToString(", ")).append("\n\n")
        }

        for (block in getActiveBlocks()) {
            when (block.type) {
                BlockType.HEADING_1 -> sb.append("# ").append(block.content).append("\n\n")
                BlockType.HEADING_2 -> sb.append("## ").append(block.content).append("\n\n")
                BlockType.HEADING_3 -> sb.append("### ").append(block.content).append("\n\n")
                BlockType.BULLET_ITEM -> sb.append("* ").append(block.content).append("\n")
                BlockType.NUMBERED_ITEM -> sb.append("1. ").append(block.content).append("\n")
                BlockType.TODO_ITEM -> sb.append("- [").append(if (block.isChecked) "x" else " ").append("] ").append(block.content).append("\n")
                BlockType.CODE_SNIPPET -> sb.append("```\n").append(block.content).append("\n```\n\n")
                BlockType.CALLOUT_NOTE -> sb.append("> **Note:** ").append(block.content).append("\n\n")
                BlockType.KEY_FORMULA -> sb.append("$$ ").append(block.content).append(" $$\n\n")
                else -> sb.append(block.content).append("\n\n")
            }
        }
        return sb.toString().trim()
    }
}

object CrdtEngine {

    fun applyOperation(note: CrdtNote, op: CrdtOperation): Boolean {
        var modified = false

        when (op.type) {
            OperationType.CREATE_NOTE -> {
                if (!op.payload.isNullOrEmpty()) {
                    modified = note.applyTitleUpdate(op.payload, op.clientId, op.lamportClock)
                }
                if (op.targetId != null && note.lessonId == null) {
                    note.lessonId = op.targetId
                    modified = true
                }
            }
            OperationType.UPDATE_TITLE -> {
                if (op.payload != null) {
                    modified = note.applyTitleUpdate(op.payload, op.clientId, op.lamportClock)
                }
            }
            OperationType.INSERT_BLOCK -> {
                val blockId = op.targetId ?: return false
                val posKey = op.payload ?: "a0"
                val contentAndType = op.secondaryPayload

                var content = ""
                var type = BlockType.PARAGRAPH

                if (contentAndType != null) {
                    val sep = contentAndType.lastIndexOf(":::")
                    if (sep != -1) {
                        content = contentAndType.substring(0, sep)
                        try {
                            type = BlockType.valueOf(contentAndType.substring(sep + 3))
                        } catch (ignored: Exception) {}
                    } else {
                        content = contentAndType
                    }
                }

                val fracPos = FractionalIndex(posKey, op.clientId, op.lamportClock)
                val existing = note.blocks[blockId]

                if (existing == null) {
                    val newBlock = CrdtBlock(
                        blockId = blockId,
                        position = fracPos,
                        type = type,
                        content = content,
                        isChecked = false,
                        isDeleted = false,
                        lamportClock = op.lamportClock,
                        clientId = op.clientId,
                        lastModified = op.timestamp
                    )
                    note.blocks[blockId] = newBlock
                    modified = true
                } else if (isNewer(op.lamportClock, op.clientId, existing.lamportClock, existing.clientId)) {
                    existing.position = fracPos
                    existing.type = type
                    existing.content = content
                    existing.lamportClock = op.lamportClock
                    existing.clientId = op.clientId
                    existing.lastModified = op.timestamp
                    modified = true
                }
            }
            OperationType.UPDATE_BLOCK_CONTENT -> {
                val block = note.blocks[op.targetId]
                if (block != null && op.payload != null) {
                    if (isNewer(op.lamportClock, op.clientId, block.lamportClock, block.clientId)) {
                        block.content = op.payload
                        block.lamportClock = op.lamportClock
                        block.clientId = op.clientId
                        block.lastModified = op.timestamp
                        modified = true
                    }
                }
            }
            OperationType.TOGGLE_BLOCK_CHECK -> {
                val block = note.blocks[op.targetId]
                if (block != null && op.payload != null) {
                    if (isNewer(op.lamportClock, op.clientId, block.lamportClock, block.clientId)) {
                        block.isChecked = op.payload.toBoolean()
                        block.lamportClock = op.lamportClock
                        block.clientId = op.clientId
                        block.lastModified = op.timestamp
                        modified = true
                    }
                }
            }
            OperationType.DELETE_BLOCK -> {
                val block = note.blocks[op.targetId]
                if (block != null && op.lamportClock >= block.lamportClock) {
                    block.isDeleted = true
                    block.lamportClock = op.lamportClock
                    block.clientId = op.clientId
                    block.lastModified = op.timestamp
                    modified = true
                }
            }
            OperationType.ADD_TAG -> {
                if (op.targetId != null) {
                    note.addTag(op.targetId, op.lamportClock, op.clientId)
                    modified = true
                }
            }
            OperationType.REMOVE_TAG -> {
                if (op.targetId != null) {
                    note.removeTag(op.targetId, op.lamportClock, op.clientId)
                    modified = true
                }
            }
            OperationType.DELETE_NOTE -> {
                note.markDeleted(op.lamportClock, op.clientId)
                modified = true
            }
            else -> {}
        }

        note.versionVector.updateMax(op.clientId, op.lamportClock)
        return modified
    }

    fun applyBatch(note: CrdtNote, ops: List<CrdtOperation>) {
        val sorted = ops.sorted()
        for (op in sorted) {
            applyOperation(note, op)
        }
    }

    private fun isNewer(clock1: Long, client1: String, clock2: Long, client2: String): Boolean {
        if (clock1 > clock2) return true
        if (clock1 < clock2) return false
        return client1 > client2
    }
}
