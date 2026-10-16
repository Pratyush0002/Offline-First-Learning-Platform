package com.studenthelper.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "courses")
public class CourseEntity implements Serializable {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "subject", length = 100)
    private String subject;

    @Column(name = "level", length = 50)
    private String level;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @Column(name = "estimated_hours")
    private int estimatedHours;

    @Column(name = "created_at")
    private long createdAt;

    public CourseEntity() {}

    public CourseEntity(String id, String title, String description, String subject, String level, String thumbnailUrl, int estimatedHours) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.subject = subject;
        this.level = level;
        this.thumbnailUrl = thumbnailUrl;
        this.estimatedHours = estimatedHours;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public int getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(int estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
