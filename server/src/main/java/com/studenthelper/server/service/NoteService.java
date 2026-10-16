package com.studenthelper.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studenthelper.crdt.model.CrdtNote;
import com.studenthelper.server.entity.NoteEntity;
import com.studenthelper.server.repository.NoteEntityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NoteService {

    private final NoteEntityRepository noteRepository;
    private final SyncService syncService;
    private final ObjectMapper objectMapper;

    public NoteService(NoteEntityRepository noteRepository, SyncService syncService, ObjectMapper objectMapper) {
        this.noteRepository = noteRepository;
        this.syncService = syncService;
        this.objectMapper = objectMapper;
    }

    public List<NoteEntity> getActiveNotes() {
        return noteRepository.findByDeletedFalse();
    }

    public List<NoteEntity> getNotesByLesson(String lessonId) {
        return noteRepository.findByLessonIdAndDeletedFalse(lessonId);
    }

    public Optional<NoteEntity> getNoteById(String id) {
        return noteRepository.findById(id);
    }

    public CrdtNote getCrdtNote(String id) {
        return syncService.rebuildNoteFromOperations(id);
    }

    public String getNoteMarkdown(String id) {
        Optional<NoteEntity> note = noteRepository.findById(id);
        if (note.isPresent()) {
            return note.get().getContentMarkdown();
        }
        CrdtNote crdt = syncService.rebuildNoteFromOperations(id);
        return crdt != null ? crdt.toMarkdown() : "# Note Not Found";
    }
}
