package com.studenthelper.server.dto;

import com.studenthelper.crdt.clock.VersionVector;
import com.studenthelper.crdt.operation.CrdtOperation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bidirectional delta synchronization request sent from offline clients when internet connects.
 */
public class SyncRequest implements Serializable {
    private String clientId;
    private String deviceName;
    private VersionVector clientVector;
    private List<CrdtOperation> operations = new ArrayList<>();

    public SyncRequest() {}

    public SyncRequest(String clientId, String deviceName, VersionVector clientVector, List<CrdtOperation> operations) {
        this.clientId = clientId;
        this.deviceName = deviceName;
        this.clientVector = clientVector != null ? clientVector : new VersionVector();
        this.operations = operations != null ? operations : new ArrayList<>();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public VersionVector getClientVector() {
        return clientVector;
    }

    public void setClientVector(VersionVector clientVector) {
        this.clientVector = clientVector;
    }

    public List<CrdtOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<CrdtOperation> operations) {
        this.operations = operations;
    }
}
