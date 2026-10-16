package com.studenthelper.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "lesson_progress", indexes = {
    @Index(name = "idx_progress_student", columnList = "student_id"),
    @Index(name = "idx_progress_lesson", columnList = "lesson_id")
})
public class LessonProgressEntity implements Serializable {

    @Id
    @Column(name = "id", nullable = false, length = 128)
    private String id; // studentId + ":" + lessonId

    @Column(name = "lesson_id", nullable = false, length = 64)
    private String lessonId;

    @Column(name = "student_id", nullable = false, length = 64)
    private String studentId;

    @Column(name = "progress_percentage")
    private int progressPercentage;

    @Column(name = "is_completed")
    private boolean completed;

    @Column(name = "quiz_score")
    private int quizScore;

    @Column(name = "last_read_block_id", length = 64)
    private String lastReadBlockId;

    @Column(name = "lamport_clock")
    private long lamportClock;

    @Column(name = "client_id", length = 64)
    private String clientId;

    @Column(name = "updated_at")
    private long updatedAt;

    public LessonProgressEntity() {}

    public LessonProgressEntity(String studentId, String lessonId, int progressPercentage, boolean completed, int quizScore, String lastReadBlockId, long lamportClock, String clientId) {
        this.id = studentId + ":" + lessonId;
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.progressPercentage = progressPercentage;
        this.completed = completed;
        this.quizScore = quizScore;
        this.lastReadBlockId = lastReadBlockId;
        this.lamportClock = lamportClock;
        this.clientId = clientId;
        this.updatedAt = System.currentTimeMillis();
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

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getQuizScore() {
        return quizScore;
    }

    public void setQuizScore(int quizScore) {
        this.quizScore = quizScore;
    }

    public String getLastReadBlockId() {
        return lastReadBlockId;
    }

    public void setLastReadBlockId(String lastReadBlockId) {
        this.lastReadBlockId = lastReadBlockId;
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

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
