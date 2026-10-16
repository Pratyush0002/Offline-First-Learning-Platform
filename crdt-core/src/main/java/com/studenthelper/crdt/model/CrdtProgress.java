package com.studenthelper.crdt.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

/**
 * CRDT representation of a student's progress on a lesson.
 * Implements monotonic merge semantics:
 * - completed: Grow-Only (once completed anywhere, remains completed)
 * - progressPercentage: Max-value merge
 * - quizScore: Max-value or latest attempt based on Lamport clock
 */
public class CrdtProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("lessonId")
    private final String lessonId;

    @JsonProperty("studentId")
    private final String studentId;

    @JsonProperty("progressPercentage")
    private int progressPercentage;

    @JsonProperty("completed")
    private boolean completed;

    @JsonProperty("quizScore")
    private int quizScore;

    @JsonProperty("lastReadBlockId")
    private String lastReadBlockId;

    @JsonProperty("lamportClock")
    private long lamportClock;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("lastUpdated")
    private long lastUpdated;

    @JsonCreator
    public CrdtProgress(
            @JsonProperty("lessonId") String lessonId,
            @JsonProperty("studentId") String studentId,
            @JsonProperty("progressPercentage") int progressPercentage,
            @JsonProperty("completed") boolean completed,
            @JsonProperty("quizScore") int quizScore,
            @JsonProperty("lastReadBlockId") String lastReadBlockId,
            @JsonProperty("lamportClock") long lamportClock,
            @JsonProperty("clientId") String clientId,
            @JsonProperty("lastUpdated") long lastUpdated) {
        this.lessonId = lessonId;
        this.studentId = studentId != null ? studentId : "student-default";
        this.progressPercentage = Math.min(100, Math.max(0, progressPercentage));
        this.completed = completed;
        this.quizScore = quizScore;
        this.lastReadBlockId = lastReadBlockId;
        this.lamportClock = lamportClock;
        this.clientId = clientId != null ? clientId : "default";
        this.lastUpdated = lastUpdated > 0 ? lastUpdated : System.currentTimeMillis();
    }

    public static CrdtProgress initial(String lessonId, String studentId, String clientId) {
        return new CrdtProgress(lessonId, studentId, 0, false, 0, null, 0, clientId, System.currentTimeMillis());
    }

    /**
     * Conflict-free monotonic merge with another progress record.
     */
    public synchronized void merge(CrdtProgress other) {
        if (other == null) return;

        // Monotonic completion: once completed on any device, stays completed
        this.completed = this.completed || other.completed;

        // Progress percentage monotonically increases
        this.progressPercentage = Math.max(this.progressPercentage, other.progressPercentage);

        // Highest score preserved
        this.quizScore = Math.max(this.quizScore, other.quizScore);

        // Last read position updated by Lamport clock
        if (other.lamportClock > this.lamportClock) {
            this.lastReadBlockId = other.lastReadBlockId;
            this.lamportClock = other.lamportClock;
            this.clientId = other.clientId;
            this.lastUpdated = Math.max(this.lastUpdated, other.lastUpdated);
        }
    }

    public String getLessonId() {
        return lessonId;
    }

    public String getStudentId() {
        return studentId;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = Math.min(100, Math.max(0, progressPercentage));
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

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrdtProgress that)) return false;
        return Objects.equals(lessonId, that.lessonId) && Objects.equals(studentId, that.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lessonId, studentId);
    }

    @Override
    public String toString() {
        return "CrdtProgress{" +
                "lesson='" + lessonId + '\'' +
                ", " + progressPercentage + "%" +
                ", completed=" + completed +
                ", score=" + quizScore +
                '}';
    }
}
