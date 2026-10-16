package com.studenthelper.crdt.operation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.studenthelper.crdt.model.BlockType;
import com.studenthelper.crdt.model.FractionalIndex;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable atomic CRDT Operation representing a state mutation.
 * Operations are totally ordered by (lamportClock, clientId, opId).
 */
public class CrdtOperation implements Comparable<CrdtOperation>, Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("opId")
    private final String opId;

    @JsonProperty("noteId")
    private final String noteId;

    @JsonProperty("type")
    private final OperationType type;

    @JsonProperty("clientId")
    private final String clientId;

    @JsonProperty("lamportClock")
    private final long lamportClock;

    @JsonProperty("timestamp")
    private final long timestamp;

    @JsonProperty("targetId")
    private final String targetId;

    @JsonProperty("payload")
    private final String payload;

    @JsonProperty("secondaryPayload")
    private final String secondaryPayload;

    @JsonCreator
    public CrdtOperation(
            @JsonProperty("opId") String opId,
            @JsonProperty("noteId") String noteId,
            @JsonProperty("type") OperationType type,
            @JsonProperty("clientId") String clientId,
            @JsonProperty("lamportClock") long lamportClock,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("targetId") String targetId,
            @JsonProperty("payload") String payload,
            @JsonProperty("secondaryPayload") String secondaryPayload) {
        this.opId = opId != null ? opId : UUID.randomUUID().toString();
        this.noteId = noteId;
        this.type = type;
        this.clientId = clientId;
        this.lamportClock = lamportClock;
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.targetId = targetId;
        this.payload = payload;
        this.secondaryPayload = secondaryPayload;
    }

    public static CrdtOperation createNote(String noteId, String lessonId, String title, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                noteId,
                OperationType.CREATE_NOTE,
                clientId,
                clock,
                System.currentTimeMillis(),
                lessonId,
                title,
                null
        );
    }

    public static CrdtOperation updateTitle(String noteId, String newTitle, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                noteId,
                OperationType.UPDATE_TITLE,
                clientId,
                clock,
                System.currentTimeMillis(),
                null,
                newTitle,
                null
        );
    }

    public static CrdtOperation insertBlock(String noteId, String blockId, FractionalIndex pos, BlockType blockType, String content, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                noteId,
                OperationType.INSERT_BLOCK,
                clientId,
                clock,
                System.currentTimeMillis(),
                blockId,
                pos.getPosition(),
                content + ":::" + (blockType != null ? blockType.name() : BlockType.PARAGRAPH.name())
        );
    }

    public static CrdtOperation updateBlockContent(String noteId, String blockId, String newContent, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                noteId,
                OperationType.UPDATE_BLOCK_CONTENT,
                clientId,
                clock,
                System.currentTimeMillis(),
                blockId,
                newContent,
                null
        );
    }

    public static CrdtOperation toggleBlockCheck(String noteId, String blockId, boolean checked, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                noteId,
                OperationType.TOGGLE_BLOCK_CHECK,
                clientId,
                clock,
                System.currentTimeMillis(),
                blockId,
                String.valueOf(checked),
                null
        );
    }

    public static CrdtOperation moveBlock(String noteId, String blockId, FractionalIndex newPos, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                noteId,
                OperationType.MOVE_BLOCK,
                clientId,
                clock,
                System.currentTimeMillis(),
                blockId,
                newPos.getPosition(),
                null
        );
    }

    public static CrdtOperation deleteBlock(String noteId, String blockId, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                noteId,
                OperationType.DELETE_BLOCK,
                clientId,
                clock,
                System.currentTimeMillis(),
                blockId,
                null,
                null
        );
    }

    public static CrdtOperation addTag(String noteId, String tag, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                noteId,
                OperationType.ADD_TAG,
                clientId,
                clock,
                System.currentTimeMillis(),
                tag,
                null,
                null
        );
    }

    public static CrdtOperation removeTag(String noteId, String tag, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                noteId,
                OperationType.REMOVE_TAG,
                clientId,
                clock,
                System.currentTimeMillis(),
                tag,
                null,
                null
        );
    }

    public static CrdtOperation deleteNote(String noteId, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                noteId,
                OperationType.DELETE_NOTE,
                clientId,
                clock,
                System.currentTimeMillis(),
                null,
                null,
                null
        );
    }

    public static CrdtOperation updateProgress(String lessonId, int progressPercent, boolean completed, int quizScore, String clientId, long clock) {
        return new CrdtOperation(
                UUID.randomUUID().toString(),
                lessonId,
                OperationType.UPDATE_PROGRESS,
                clientId,
                clock,
                System.currentTimeMillis(),
                lessonId,
                progressPercent + ":" + completed + ":" + quizScore,
                null
        );
    }

    public String getOpId() {
        return opId;
    }

    public String getNoteId() {
        return noteId;
    }

    public OperationType getType() {
        return type;
    }

    public String getClientId() {
        return clientId;
    }

    public long getLamportClock() {
        return lamportClock;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getPayload() {
        return payload;
    }

    public String getSecondaryPayload() {
        return secondaryPayload;
    }

    @Override
    public int compareTo(CrdtOperation o) {
        if (o == null) return 1;
        // Total causal ordering:
        // 1. Lamport clock
        int clockCmp = Long.compare(this.lamportClock, o.lamportClock);
        if (clockCmp != 0) return clockCmp;
        // 2. Client ID tie breaker
        int clientCmp = this.clientId.compareTo(o.clientId);
        if (clientCmp != 0) return clientCmp;
        // 3. Operation ID tie breaker
        return this.opId.compareTo(o.opId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrdtOperation that)) return false;
        return Objects.equals(opId, that.opId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opId);
    }

    @Override
    public String toString() {
        return "CrdtOp{" +
                "opId='" + opId.substring(0, Math.min(8, opId.length())) + '\'' +
                ", type=" + type +
                ", client='" + clientId + '\'' +
                ", clock=" + lamportClock +
                ", target='" + targetId + '\'' +
                '}';
    }
}
