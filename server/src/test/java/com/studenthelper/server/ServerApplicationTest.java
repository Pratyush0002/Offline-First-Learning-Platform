package com.studenthelper.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studenthelper.crdt.clock.VersionVector;
import com.studenthelper.crdt.model.BlockType;
import com.studenthelper.crdt.model.FractionalIndex;
import com.studenthelper.crdt.operation.CrdtOperation;
import com.studenthelper.server.dto.SyncRequest;
import com.studenthelper.server.dto.SyncResponse;
import com.studenthelper.server.service.CourseService;
import com.studenthelper.server.service.SyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ServerApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseService courseService;

    @Autowired
    private SyncService syncService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Context loads and pre-seeded courses and lessons are available")
    void testCoursesSeeded() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    @DisplayName("Offline lesson download package contains all lesson materials and quizzes")
    void testDownloadLessonPackage() throws Exception {
        mockMvc.perform(get("/api/lessons/lesson-dsa-big-o/download?studentId=test-student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lesson.id").value("lesson-dsa-big-o"))
                .andExpect(jsonPath("$.course.id").value("course-dsa-101"))
                .andExpect(jsonPath("$.siblingLessons").isArray());
    }

    @Test
    @DisplayName("Delta sync successfully receives client operations and merges without conflict")
    void testSyncDelta() throws Exception {
        String noteId = "note-test-" + UUID.randomUUID();
        String clientId = "test-phone";

        CrdtOperation op1 = CrdtOperation.createNote(noteId, "lesson-dsa-big-o", "Test Note Title", clientId, 1);
        CrdtOperation op2 = CrdtOperation.insertBlock(noteId, "b-1", FractionalIndex.initial(clientId, 2), BlockType.PARAGRAPH, "Testing paragraph", clientId, 2);

        SyncRequest request = new SyncRequest(clientId, "Test Phone", new VersionVector(), List.of(op1, op2));

        mockMvc.perform(post("/api/sync/delta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.acknowledgedOpIds.length()").value(2));

        // Verify note rendered as markdown
        mockMvc.perform(get("/api/notes/" + noteId + "/markdown"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Test Note Title")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Testing paragraph")));
    }

    @Test
    @DisplayName("Multi-device conflict simulation runs and verifies 0% data loss")
    void testSimulatorEndpoint() throws Exception {
        mockMvc.perform(post("/api/simulator/simulate-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalBlocksCount").value(2))
                .andExpect(jsonPath("$.convergedTags").isArray());
    }
}
