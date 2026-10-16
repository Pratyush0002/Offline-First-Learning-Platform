package com.studenthelper.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "lessons", indexes = {
    @Index(name = "idx_lesson_course", columnList = "course_id")
})
public class LessonEntity implements Serializable {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "course_id", nullable = false, length = 64)
    private String courseId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "order_index")
    private int orderIndex;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content_markdown", columnDefinition = "TEXT")
    private String contentMarkdown;

    @Column(name = "estimated_minutes")
    private int estimatedMinutes;

    @Column(name = "quiz_questions_json", columnDefinition = "TEXT")
    private String quizQuestionsJson;

    @Column(name = "resources_json", columnDefinition = "TEXT")
    private String resourcesJson;

    public LessonEntity() {}

    public LessonEntity(String id, String courseId, String title, int orderIndex, String summary, String contentMarkdown, int estimatedMinutes, String quizQuestionsJson, String resourcesJson) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.orderIndex = orderIndex;
        this.summary = summary;
        this.contentMarkdown = contentMarkdown;
        this.estimatedMinutes = estimatedMinutes;
        this.quizQuestionsJson = quizQuestionsJson;
        this.resourcesJson = resourcesJson;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContentMarkdown() {
        return contentMarkdown;
    }

    public void setContentMarkdown(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public String getQuizQuestionsJson() {
        return quizQuestionsJson;
    }

    public void setQuizQuestionsJson(String quizQuestionsJson) {
        this.quizQuestionsJson = quizQuestionsJson;
    }

    public String getResourcesJson() {
        return resourcesJson;
    }

    public void setResourcesJson(String resourcesJson) {
        this.resourcesJson = resourcesJson;
    }
}
