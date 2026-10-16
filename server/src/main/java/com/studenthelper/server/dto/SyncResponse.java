package com.studenthelper.server.dto;

import com.studenthelper.crdt.clock.VersionVector;
import com.studenthelper.crdt.model.CrdtNote;
import com.studenthelper.crdt.operation.CrdtOperation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Server response to a delta synchronization handshake.
 */
public class SyncResponse implements Serializable {
    private String status = "SUCCESS";
    private List<String> acknowledgedOpIds = new ArrayList<>();
    private List<CrdtOperation> missingOperations = new ArrayList<>();
    private VersionVector serverVector;
    private List<CrdtNote> updatedNotes = new ArrayList<>();
    private long serverTimestamp;

    public SyncResponse() {
        this.serverTimestamp = System.currentTimeMillis();
    }

    public SyncResponse(String status, List<String> acknowledgedOpIds, List<CrdtOperation> missingOperations, VersionVector serverVector) {
        this.status = status;
        this.acknowledgedOpIds = acknowledgedOpIds != null ? acknowledgedOpIds : new ArrayList<>();
        this.missingOperations = missingOperations != null ? missingOperations : new ArrayList<>();
        this.serverVector = serverVector != null ? serverVector : new VersionVector();
        this.serverTimestamp = System.currentTimeMillis();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getAcknowledgedOpIds() {
        return acknowledgedOpIds;
    }

    public void setAcknowledgedOpIds(List<String> acknowledgedOpIds) {
        this.acknowledgedOpIds = acknowledgedOpIds;
    }

    public List<CrdtOperation> getMissingOperations() {
        return missingOperations;
    }

    public void setMissingOperations(List<CrdtOperation> missingOperations) {
        this.missingOperations = missingOperations;
    }

    public VersionVector getServerVector() {
        return serverVector;
    }

    public void setServerVector(VersionVector serverVector) {
        this.serverVector = serverVector;
    }

    public List<CrdtNote> getUpdatedNotes() {
        return updatedNotes;
    }

    public void setUpdatedNotes(List<CrdtNote> updatedNotes) {
        this.updatedNotes = updatedNotes;
    }

    public long getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(long serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }
}
