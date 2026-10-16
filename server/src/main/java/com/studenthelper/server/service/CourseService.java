package com.studenthelper.server.service;

import com.studenthelper.server.dto.CourseWithLessonsDto;
import com.studenthelper.server.dto.LessonPackageDto;
import com.studenthelper.server.entity.CourseEntity;
import com.studenthelper.server.entity.LessonEntity;
import com.studenthelper.server.entity.LessonProgressEntity;
import com.studenthelper.server.repository.CourseRepository;
import com.studenthelper.server.repository.LessonProgressRepository;
import com.studenthelper.server.repository.LessonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;

    public CourseService(CourseRepository courseRepository, LessonRepository lessonRepository, LessonProgressRepository progressRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
    }

    public List<CourseEntity> getAllCourses() {
        return courseRepository.findAll();
    }

    public Optional<CourseEntity> getCourseById(String id) {
        return courseRepository.findById(id);
    }

    public CourseWithLessonsDto getCourseWithLessons(String courseId, String studentId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        List<LessonEntity> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        int completedCount = 0;
        int totalProgressSum = 0;

        for (LessonEntity lesson : lessons) {
            Optional<LessonProgressEntity> progressOpt = progressRepository.findByStudentIdAndLessonId(studentId, lesson.getId());
            if (progressOpt.isPresent()) {
                LessonProgressEntity p = progressOpt.get();
                if (p.isCompleted()) completedCount++;
                totalProgressSum += p.getProgressPercentage();
            }
        }

        int overallPercent = lessons.isEmpty() ? 0 : (totalProgressSum / lessons.size());
        return new CourseWithLessonsDto(course, lessons, completedCount, overallPercent);
    }

    public LessonEntity getLesson(String lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId));
    }

    public List<LessonEntity> getLessonsByCourse(String courseId) {
        return lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    /**
     * Prepares an offline bundle package for downloading to an Android device.
     */
    public LessonPackageDto downloadLessonPackage(String lessonId, String studentId) {
        LessonEntity lesson = getLesson(lessonId);
        CourseEntity course = courseRepository.findById(lesson.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found for lesson: " + lessonId));

        LessonProgressEntity progress = progressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .orElseGet(() -> new LessonProgressEntity(studentId, lessonId, 0, false, 0, null, 0, "server"));

        List<LessonEntity> siblingLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(lesson.getCourseId());

        return new LessonPackageDto(course, lesson, progress, siblingLessons);
    }
}
