package com.studenthelper.server.controller;

import com.studenthelper.crdt.model.CrdtNote;
import com.studenthelper.server.entity.NoteEntity;
import com.studenthelper.server.service.NoteService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public ResponseEntity<List<NoteEntity>> getAllNotes() {
        return ResponseEntity.ok(noteService.getActiveNotes());
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<NoteEntity>> getNotesByLesson(@PathVariable("lessonId") String lessonId) {
        return ResponseEntity.ok(noteService.getNotesByLesson(lessonId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteEntity> getNoteById(@PathVariable("id") String id) {
        return noteService.getNoteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/crdt")
    public ResponseEntity<CrdtNote> getCrdtNote(@PathVariable("id") String id) {
        CrdtNote note = noteService.getCrdtNote(id);
        if (note != null) {
            return ResponseEntity.ok(note);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/{id}/markdown", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public ResponseEntity<String> getNoteMarkdown(@PathVariable("id") String id) {
        return ResponseEntity.ok(noteService.getNoteMarkdown(id));
    }
}
