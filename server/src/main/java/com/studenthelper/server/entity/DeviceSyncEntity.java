package com.studenthelper.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "device_sync_states")
public class DeviceSyncEntity implements Serializable {

    @Id
    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "last_sync_timestamp")
    private long lastSyncTimestamp;

    @Column(name = "max_lamport_clock")
    private long maxLamportClock;

    public DeviceSyncEntity() {}

    public DeviceSyncEntity(String clientId, String deviceName, long lastSyncTimestamp, long maxLamportClock) {
        this.clientId = clientId;
        this.deviceName = deviceName;
        this.lastSyncTimestamp = lastSyncTimestamp;
        this.maxLamportClock = maxLamportClock;
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

    public long getLastSyncTimestamp() {
        return lastSyncTimestamp;
    }

    public void setLastSyncTimestamp(long lastSyncTimestamp) {
        this.lastSyncTimestamp = lastSyncTimestamp;
    }

    public long getMaxLamportClock() {
        return maxLamportClock;
    }

    public void setMaxLamportClock(long maxLamportClock) {
        this.maxLamportClock = maxLamportClock;
    }
}
