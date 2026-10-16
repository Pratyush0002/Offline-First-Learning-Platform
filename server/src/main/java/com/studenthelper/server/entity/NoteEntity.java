package com.studenthelper.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "notes", indexes = {
    @Index(name = "idx_notes_lesson", columnList = "lesson_id"),
    @Index(name = "idx_notes_author", columnList = "author_id")
})
public class NoteEntity implements Serializable {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "lesson_id", length = 64)
    private String lessonId;

    @Column(name = "author_id", length = 64)
    private String authorId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "title_clock")
    private long titleClock;

    @Column(name = "title_client_id", length = 64)
    private String titleClientId;

    @Column(name = "content_markdown", columnDefinition = "TEXT")
    private String contentMarkdown;

    @Column(name = "is_deleted")
    private boolean deleted;

    @Column(name = "delete_clock")
    private long deleteClock;

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    @Column(name = "crdt_state_json", columnDefinition = "TEXT")
    private String crdtStateJson;

    @Column(name = "created_at")
    private long createdAt;

    @Column(name = "updated_at")
    private long updatedAt;

    public NoteEntity() {}

    public NoteEntity(String id, String lessonId, String authorId, String title, long titleClock, String titleClientId, String contentMarkdown, boolean deleted, long deleteClock, String tagsJson, String crdtStateJson, long createdAt, long updatedAt) {
        this.id = id;
        this.lessonId = lessonId;
        this.authorId = authorId;
        this.title = title;
        this.titleClock = titleClock;
        this.titleClientId = titleClientId;
        this.contentMarkdown = contentMarkdown;
        this.deleted = deleted;
        this.deleteClock = deleteClock;
        this.tagsJson = tagsJson;
        this.crdtStateJson = crdtStateJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTitleClock() {
        return titleClock;
    }

    public void setTitleClock(long titleClock) {
        this.titleClock = titleClock;
    }

    public String getTitleClientId() {
        return titleClientId;
    }

    public void setTitleClientId(String titleClientId) {
        this.titleClientId = titleClientId;
    }

    public String getContentMarkdown() {
        return contentMarkdown;
    }

    public void setContentMarkdown(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public long getDeleteClock() {
        return deleteClock;
    }

    public void setDeleteClock(long deleteClock) {
        this.deleteClock = deleteClock;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }

    public String getCrdtStateJson() {
        return crdtStateJson;
    }

    public void setCrdtStateJson(String crdtStateJson) {
        this.crdtStateJson = crdtStateJson;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
