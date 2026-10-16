package com.studenthelper.crdt.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

/**
 * An individual content block within a CRDT Note (e.g. paragraph, heading, todo item).
 * Blocks are ordered via FractionalIndex and track modifications using Lamport timestamps.
 */
public class CrdtBlock implements Comparable<CrdtBlock>, Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("blockId")
    private final String blockId;

    @JsonProperty("position")
    private FractionalIndex position;

    @JsonProperty("type")
    private BlockType type;

    @JsonProperty("content")
    private String content;

    @JsonProperty("checked")
    private boolean checked;

    @JsonProperty("deleted")
    private boolean deleted;

    @JsonProperty("lamportClock")
    private long lamportClock;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("lastModified")
    private long lastModified;

    @JsonCreator
    public CrdtBlock(
            @JsonProperty("blockId") String blockId,
            @JsonProperty("position") FractionalIndex position,
            @JsonProperty("type") BlockType type,
            @JsonProperty("content") String content,
            @JsonProperty("checked") boolean checked,
            @JsonProperty("deleted") boolean deleted,
            @JsonProperty("lamportClock") long lamportClock,
            @JsonProperty("clientId") String clientId,
            @JsonProperty("lastModified") long lastModified) {
        this.blockId = blockId;
        this.position = position;
        this.type = type != null ? type : BlockType.PARAGRAPH;
        this.content = content != null ? content : "";
        this.checked = checked;
        this.deleted = deleted;
        this.lamportClock = lamportClock;
        this.clientId = clientId != null ? clientId : "default";
        this.lastModified = lastModified;
    }

    public static CrdtBlock create(String blockId, FractionalIndex position, BlockType type, String content, String clientId, long clock) {
        return new CrdtBlock(
                blockId,
                position,
                type,
                content,
                false,
                false,
                clock,
                clientId,
                System.currentTimeMillis()
        );
    }

    public String getBlockId() {
        return blockId;
    }

    public FractionalIndex getPosition() {
        return position;
    }

    public void setPosition(FractionalIndex position) {
        this.position = position;
    }

    public BlockType getType() {
        return type;
    }

    public void setType(BlockType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public long getLamportClock() {
        return lamportClock;
    }

    public void setLamportClock(long lamportClock) {
        this.lamportClock = lamportClock;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public int compareTo(CrdtBlock o) {
        if (o == null) return 1;
        int posCmp = this.position.compareTo(o.position);
        if (posCmp != 0) return posCmp;
        return this.blockId.compareTo(o.blockId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrdtBlock crdtBlock)) return false;
        return Objects.equals(blockId, crdtBlock.blockId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockId);
    }

    @Override
    public String toString() {
        return "CrdtBlock{" +
                "id='" + blockId + '\'' +
                ", pos=" + position +
                ", type=" + type +
                ", content='" + content + '\'' +
                ", del=" + deleted +
                '}';
    }
}
