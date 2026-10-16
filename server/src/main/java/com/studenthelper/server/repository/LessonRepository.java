package com.studenthelper.server.repository;

import com.studenthelper.server.entity.LessonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<LessonEntity, String> {
    List<LessonEntity> findByCourseIdOrderByOrderIndexAsc(String courseId);
}
