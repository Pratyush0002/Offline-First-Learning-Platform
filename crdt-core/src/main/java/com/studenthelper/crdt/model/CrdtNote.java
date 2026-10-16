package com.studenthelper.crdt.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.studenthelper.crdt.clock.VersionVector;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Conflict-free Replicated Data Type representing a Student Note.
 * Combines LWW-Registers (for title and deletion), Sequence CRDT with Fractional Indexing
 * (for content blocks), and an Observed-Remove Set (for categorization tags).
 */
public class CrdtNote implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private final String id;

    @JsonProperty("lessonId")
    private String lessonId;

    @JsonProperty("authorId")
    private String authorId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("titleClock")
    private long titleClock;

    @JsonProperty("titleClientId")
    private String titleClientId;

    @JsonProperty("blocks")
    private final Map<String, CrdtBlock> blocks;

    @JsonProperty("tags")
    private final Map<String, TagRecord> tags;

    @JsonProperty("deleted")
    private boolean deleted;

    @JsonProperty("deleteClock")
    private long deleteClock;

    @JsonProperty("createdAt")
    private long createdAt;

    @JsonProperty("updatedAt")
    private long updatedAt;

    @JsonProperty("versionVector")
    private final VersionVector versionVector;

    public static class TagRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        public long addedClock = 0;
        public long removedClock = 0;

        public TagRecord() {}

        public TagRecord(long addedClock, long removedClock) {
            this.addedClock = addedClock;
            this.removedClock = removedClock;
        }

        public boolean isActive() {
            return addedClock > removedClock;
        }
    }

    public CrdtNote(String id, String lessonId, String authorId, String title, String clientId, long clock) {
        this.id = id;
        this.lessonId = lessonId;
        this.authorId = authorId != null ? authorId : "student";
        this.title = title != null ? title : "Untitled Note";
        this.titleClock = clock;
        this.titleClientId = clientId;
        this.blocks = new ConcurrentHashMap<>();
        this.tags = new ConcurrentHashMap<>();
        this.deleted = false;
        this.deleteClock = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.versionVector = new VersionVector();
        this.versionVector.updateMax(clientId, clock);
    }

    @JsonCreator
    public CrdtNote(
            @JsonProperty("id") String id,
            @JsonProperty("lessonId") String lessonId,
            @JsonProperty("authorId") String authorId,
            @JsonProperty("title") String title,
            @JsonProperty("titleClock") long titleClock,
            @JsonProperty("titleClientId") String titleClientId,
            @JsonProperty("blocks") Map<String, CrdtBlock> blocks,
            @JsonProperty("tags") Map<String, TagRecord> tags,
            @JsonProperty("deleted") boolean deleted,
            @JsonProperty("deleteClock") long deleteClock,
            @JsonProperty("createdAt") long createdAt,
            @JsonProperty("updatedAt") long updatedAt,
            @JsonProperty("versionVector") VersionVector versionVector) {
        this.id = id;
        this.lessonId = lessonId;
        this.authorId = authorId;
        this.title = title;
        this.titleClock = titleClock;
        this.titleClientId = titleClientId;
        this.blocks = blocks != null ? new ConcurrentHashMap<>(blocks) : new ConcurrentHashMap<>();
        this.tags = tags != null ? new ConcurrentHashMap<>(tags) : new ConcurrentHashMap<>();
        this.deleted = deleted;
        this.deleteClock = deleteClock;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.updatedAt = updatedAt > 0 ? updatedAt : System.currentTimeMillis();
        this.versionVector = versionVector != null ? versionVector : new VersionVector();
    }

    public CrdtNote copy() {
        Map<String, CrdtBlock> copyBlocks = new ConcurrentHashMap<>();
        for (Map.Entry<String, CrdtBlock> e : this.blocks.entrySet()) {
            CrdtBlock b = e.getValue();
            copyBlocks.put(e.getKey(), new CrdtBlock(
                    b.getBlockId(),
                    b.getPosition(),
                    b.getType(),
                    b.getContent(),
                    b.isChecked(),
                    b.isDeleted(),
                    b.getLamportClock(),
                    b.getClientId(),
                    b.getLastModified()
            ));
        }
        Map<String, TagRecord> copyTags = new ConcurrentHashMap<>();
        for (Map.Entry<String, TagRecord> e : this.tags.entrySet()) {
            copyTags.put(e.getKey(), new TagRecord(e.getValue().addedClock, e.getValue().removedClock));
        }
        return new CrdtNote(
                this.id,
                this.lessonId,
                this.authorId,
                this.title,
                this.titleClock,
                this.titleClientId,
                copyBlocks,
                copyTags,
                this.deleted,
                this.deleteClock,
                this.createdAt,
                this.updatedAt,
                this.versionVector.clone()
        );
    }

    public String getId() {
        return id;
    }

    public String getLessonId() {
        return lessonId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public long getTitleClock() {
        return titleClock;
    }

    public String getTitleClientId() {
        return titleClientId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public long getDeleteClock() {
        return deleteClock;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public VersionVector getVersionVector() {
        return versionVector;
    }

    public Map<String, CrdtBlock> getBlocks() {
        return blocks;
    }

    public Map<String, TagRecord> getTags() {
        return tags;
    }

    /**
     * Updates note title using Last-Write-Wins (LWW) with deterministic tie-breaking.
     */
    public boolean applyTitleUpdate(String newTitle, String clientId, long clock) {
        if (clock > this.titleClock || (clock == this.titleClock && clientId.compareTo(this.titleClientId) > 0)) {
            this.title = newTitle;
            this.titleClock = clock;
            this.titleClientId = clientId;
            this.updatedAt = System.currentTimeMillis();
            this.versionVector.updateMax(clientId, clock);
            return true;
        }
        return false;
    }

    /**
     * Returns all active (non-tombstoned) blocks in deterministic fractional index order.
     */
    public List<CrdtBlock> getActiveBlocks() {
        return blocks.values().stream()
                .filter(b -> !b.isDeleted())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns all active tags.
     */
    public Set<String> getActiveTags() {
        return tags.entrySet().stream()
                .filter(e -> e.getValue().isActive())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Adds a tag using LWW-Element-Set.
     */
    public void addTag(String tag, long clock, String clientId) {
        TagRecord record = tags.computeIfAbsent(tag, k -> new TagRecord());
        if (clock > record.addedClock) {
            record.addedClock = clock;
            this.updatedAt = System.currentTimeMillis();
            this.versionVector.updateMax(clientId, clock);
        }
    }

    /**
     * Removes a tag using LWW-Element-Set tombstone clock.
     */
    public void removeTag(String tag, long clock, String clientId) {
        TagRecord record = tags.computeIfAbsent(tag, k -> new TagRecord());
        if (clock > record.removedClock) {
            record.removedClock = clock;
            this.updatedAt = System.currentTimeMillis();
            this.versionVector.updateMax(clientId, clock);
        }
    }

    /**
     * Marks the note as deleted using LWW tombstone.
     */
    public void markDeleted(long clock, String clientId) {
        if (clock > this.deleteClock) {
            this.deleted = true;
            this.deleteClock = clock;
            this.updatedAt = System.currentTimeMillis();
            this.versionVector.updateMax(clientId, clock);
        }
    }

    /**
     * Converts note content into a formatted Markdown document.
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");

        Set<String> activeTags = getActiveTags();
        if (!activeTags.isEmpty()) {
            sb.append("**Tags:** ").append(String.join(", ", activeTags)).append("\n\n");
        }

        for (CrdtBlock block : getActiveBlocks()) {
            switch (block.getType()) {
                case HEADING_1 -> sb.append("# ").append(block.getContent()).append("\n\n");
                case HEADING_2 -> sb.append("## ").append(block.getContent()).append("\n\n");
                case HEADING_3 -> sb.append("### ").append(block.getContent()).append("\n\n");
                case BULLET_ITEM -> sb.append("* ").append(block.getContent()).append("\n");
                case NUMBERED_ITEM -> sb.append("1. ").append(block.getContent()).append("\n");
                case TODO_ITEM -> sb.append("- [").append(block.isChecked() ? "x" : " ").append("] ").append(block.getContent()).append("\n");
                case CODE_SNIPPET -> sb.append("```\n").append(block.getContent()).append("\n```\n\n");
                case CALLOUT_NOTE -> sb.append("> **Note:** ").append(block.getContent()).append("\n\n");
                case KEY_FORMULA -> sb.append("$$ ").append(block.getContent()).append(" $$\n\n");
                default -> sb.append(block.getContent()).append("\n\n");
            }
        }
        return sb.toString().trim();
    }
}
