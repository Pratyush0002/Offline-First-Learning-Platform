package com.studenthelper.server.repository;

import com.studenthelper.server.entity.LessonProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgressEntity, String> {
    List<LessonProgressEntity> findByStudentId(String studentId);
    Optional<LessonProgressEntity> findByStudentIdAndLessonId(String studentId, String lessonId);
}
