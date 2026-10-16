package com.studenthelper.server.dto;

import com.studenthelper.server.entity.CourseEntity;
import com.studenthelper.server.entity.LessonEntity;

import java.io.Serializable;
import java.util.List;

public class CourseWithLessonsDto implements Serializable {
    private CourseEntity course;
    private List<LessonEntity> lessons;
    private int completedLessonsCount;
    private int overallProgressPercent;

    public CourseWithLessonsDto() {}

    public CourseWithLessonsDto(CourseEntity course, List<LessonEntity> lessons, int completedLessonsCount, int overallProgressPercent) {
        this.course = course;
        this.lessons = lessons;
        this.completedLessonsCount = completedLessonsCount;
        this.overallProgressPercent = overallProgressPercent;
    }

    public CourseEntity getCourse() {
        return course;
    }

    public void setCourse(CourseEntity course) {
        this.course = course;
    }

    public List<LessonEntity> getLessons() {
        return lessons;
    }

    public void setLessons(List<LessonEntity> lessons) {
        this.lessons = lessons;
    }

    public int getCompletedLessonsCount() {
        return completedLessonsCount;
    }

    public void setCompletedLessonsCount(int completedLessonsCount) {
        this.completedLessonsCount = completedLessonsCount;
    }

    public int getOverallProgressPercent() {
        return overallProgressPercent;
    }

    public void setOverallProgressPercent(int overallProgressPercent) {
        this.overallProgressPercent = overallProgressPercent;
    }
}
