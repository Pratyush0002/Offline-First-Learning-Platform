package com.studenthelper.server.repository;

import com.studenthelper.server.entity.NoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteEntityRepository extends JpaRepository<NoteEntity, String> {
    List<NoteEntity> findByLessonIdAndDeletedFalse(String lessonId);
    List<NoteEntity> findByAuthorIdAndDeletedFalse(String authorId);
    List<NoteEntity> findByDeletedFalse();
}
