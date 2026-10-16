package com.studenthelper.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "crdt_operations", indexes = {
    @Index(name = "idx_op_note", columnList = "note_id"),
    @Index(name = "idx_op_client_clock", columnList = "client_id, lamport_clock")
})
public class CrdtOperationEntity implements Serializable {

    @Id
    @Column(name = "op_id", nullable = false, length = 64)
    private String opId;

    @Column(name = "note_id", nullable = false, length = 64)
    private String noteId;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "lamport_clock", nullable = false)
    private long lamportClock;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "secondary_payload", columnDefinition = "TEXT")
    private String secondaryPayload;

    @Column(name = "received_at", nullable = false)
    private long receivedAt;

    public CrdtOperationEntity() {}

    public CrdtOperationEntity(String opId, String noteId, String type, String clientId, long lamportClock, long timestamp, String targetId, String payload, String secondaryPayload) {
        this.opId = opId;
        this.noteId = noteId;
        this.type = type;
        this.clientId = clientId;
        this.lamportClock = lamportClock;
        this.timestamp = timestamp;
        this.targetId = targetId;
        this.payload = payload;
        this.secondaryPayload = secondaryPayload;
        this.receivedAt = System.currentTimeMillis();
    }

    public String getOpId() {
        return opId;
    }

    public void setOpId(String opId) {
        this.opId = opId;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getLamportClock() {
        return lamportClock;
    }

    public void setLamportClock(long lamportClock) {
        this.lamportClock = lamportClock;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSecondaryPayload() {
        return secondaryPayload;
    }

    public void setSecondaryPayload(String secondaryPayload) {
        this.secondaryPayload = secondaryPayload;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }
}
