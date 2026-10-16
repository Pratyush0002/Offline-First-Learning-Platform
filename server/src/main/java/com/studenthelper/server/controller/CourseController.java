package com.studenthelper.server.controller;

import com.studenthelper.server.dto.CourseWithLessonsDto;
import com.studenthelper.server.dto.LessonPackageDto;
import com.studenthelper.server.entity.CourseEntity;
import com.studenthelper.server.entity.LessonEntity;
import com.studenthelper.server.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseEntity>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<CourseWithLessonsDto> getCourse(
            @PathVariable("id") String id,
            @RequestParam(name = "studentId", defaultValue = "student-default") String studentId) {
        return ResponseEntity.ok(courseService.getCourseWithLessons(id, studentId));
    }

    @GetMapping("/lessons/{id}")
    public ResponseEntity<LessonEntity> getLesson(@PathVariable("id") String id) {
        return ResponseEntity.ok(courseService.getLesson(id));
    }

    /**
     * Endpoint for downloading full lesson package for 100% offline usage on Android.
     */
    @GetMapping("/lessons/{id}/download")
    public ResponseEntity<LessonPackageDto> downloadLessonPackage(
            @PathVariable("id") String id,
            @RequestParam(name = "studentId", defaultValue = "student-default") String studentId) {
        return ResponseEntity.ok(courseService.downloadLessonPackage(id, studentId));
    }
}
