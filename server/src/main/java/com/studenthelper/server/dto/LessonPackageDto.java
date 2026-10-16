package com.studenthelper.server.dto;

import com.studenthelper.server.entity.CourseEntity;
import com.studenthelper.server.entity.LessonEntity;
import com.studenthelper.server.entity.LessonProgressEntity;

import java.io.Serializable;
import java.util.List;

/**
 * Complete offline bundle containing course, lesson content, quizzes, and attachments
 * downloaded to the student's Android device for offline usage.
 */
public class LessonPackageDto implements Serializable {
    private CourseEntity course;
    private LessonEntity lesson;
    private LessonProgressEntity progress;
    private List<LessonEntity> siblingLessons;
    private long downloadedAt;

    public LessonPackageDto() {
        this.downloadedAt = System.currentTimeMillis();
    }

    public LessonPackageDto(CourseEntity course, LessonEntity lesson, LessonProgressEntity progress, List<LessonEntity> siblingLessons) {
        this.course = course;
        this.lesson = lesson;
        this.progress = progress;
        this.siblingLessons = siblingLessons;
        this.downloadedAt = System.currentTimeMillis();
    }

    public CourseEntity getCourse() {
        return course;
    }

    public void setCourse(CourseEntity course) {
        this.course = course;
    }

    public LessonEntity getLesson() {
        return lesson;
    }

    public void setLesson(LessonEntity lesson) {
        this.lesson = lesson;
    }

    public LessonProgressEntity getProgress() {
        return progress;
    }

    public void setProgress(LessonProgressEntity progress) {
        this.progress = progress;
    }

    public List<LessonEntity> getSiblingLessons() {
        return siblingLessons;
    }

    public void setSiblingLessons(List<LessonEntity> siblingLessons) {
        this.siblingLessons = siblingLessons;
    }

    public long getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloadedAt(long downloadedAt) {
        this.downloadedAt = downloadedAt;
    }
}
