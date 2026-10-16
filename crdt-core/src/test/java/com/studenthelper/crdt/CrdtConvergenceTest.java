package com.studenthelper.crdt;

import com.studenthelper.crdt.clock.LamportClock;
import com.studenthelper.crdt.clock.VersionVector;
import com.studenthelper.crdt.engine.CrdtEngine;
import com.studenthelper.crdt.model.*;
import com.studenthelper.crdt.operation.CrdtOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CrdtConvergenceTest {

    @Test
    @DisplayName("Fractional indexing correctly orders elements inserted between existing blocks")
    void testFractionalIndexing() {
        FractionalIndex first = FractionalIndex.initial("device-1", 1);
        FractionalIndex last = FractionalIndex.between(first, null, "device-1", 2);
        assertTrue(first.compareTo(last) < 0, "Initial block should be before last block");

        // Insert between first and last
        FractionalIndex middle = FractionalIndex.between(first, last, "device-2", 3);
        assertTrue(first.compareTo(middle) < 0, "First should be before middle");
        assertTrue(middle.compareTo(last) < 0, "Middle should be before last");

        // Insert between first and middle
        FractionalIndex quarter = FractionalIndex.between(first, middle, "device-1", 4);
        assertTrue(first.compareTo(quarter) < 0);
        assertTrue(quarter.compareTo(middle) < 0);
    }

    @Test
    @DisplayName("Two devices editing the same note offline converge conflict-free upon synchronization")
    void testOfflineConcurrentEditingConvergence() {
        String noteId = "note-dsa-101";
        String lessonId = "lesson-graph-traversal";

        // Initial state on Server
        LamportClock serverClock = new LamportClock(1);
        CrdtNote serverNote = new CrdtNote(noteId, lessonId, "student-1", "Graph Algorithms", "server", serverClock.tick());

        // Initial block on server
        String initialBlockId = "b0";
        FractionalIndex b0Pos = FractionalIndex.initial("server", serverClock.tick());
        CrdtOperation b0Op = CrdtOperation.insertBlock(noteId, initialBlockId, b0Pos, BlockType.HEADING_1, "Graphs Overview", "server", serverClock.tick());
        CrdtEngine.applyOperation(serverNote, b0Op);

        // Device A (Phone) and Device B (Tablet) both download the note
        CrdtNote deviceANote = CrdtEngine.merge(null, serverNote);
        CrdtNote deviceBNote = CrdtEngine.merge(null, serverNote);

        LamportClock clockA = new LamportClock(serverClock.get());
        LamportClock clockB = new LamportClock(serverClock.get());

        // --- SIMULATE OFFLINE PERIOD ---
        // Device A is offline at the train station:
        // A adds a new paragraph explaining BFS after b0
        String blockAId = "b-bfs";
        FractionalIndex bA_pos = FractionalIndex.between(b0Pos, null, "device-phone", clockA.tick());
        CrdtOperation opA1 = CrdtOperation.insertBlock(noteId, blockAId, bA_pos, BlockType.PARAGRAPH, "BFS uses a Queue to visit nodes level by level.", "device-phone", clockA.tick());
        CrdtOperation opA2 = CrdtOperation.addTag(noteId, "BFS", "device-phone", clockA.tick());
        CrdtEngine.applyOperation(deviceANote, opA1);
        CrdtEngine.applyOperation(deviceANote, opA2);

        // Device B is offline in the classroom:
        // B adds a new paragraph explaining DFS after b0, updates title, and adds tag "DFS"
        String blockBId = "b-dfs";
        FractionalIndex bB_pos = FractionalIndex.between(b0Pos, null, "device-tablet", clockB.tick());
        CrdtOperation opB1 = CrdtOperation.insertBlock(noteId, blockBId, bB_pos, BlockType.PARAGRAPH, "DFS uses a Stack or recursion to explore depth first.", "device-tablet", clockB.tick());
        CrdtOperation opB2 = CrdtOperation.updateTitle(noteId, "Graph Traversal & Search Algorithms", "device-tablet", clockB.tick());
        CrdtOperation opB3 = CrdtOperation.addTag(noteId, "DFS", "device-tablet", clockB.tick());
        CrdtEngine.applyOperation(deviceBNote, opB1);
        CrdtEngine.applyOperation(deviceBNote, opB2);
        CrdtEngine.applyOperation(deviceBNote, opB3);

        // Verify that offline states are distinct before sync
        assertEquals(2, deviceANote.getActiveBlocks().size());
        assertEquals(2, deviceBNote.getActiveBlocks().size());
        assertFalse(deviceANote.getActiveTags().contains("DFS"));
        assertFalse(deviceBNote.getActiveTags().contains("BFS"));

        // --- SIMULATE INTERNET RETURNS & SYNC HAPPENS ---
        // Sync Device A -> Server
        List<CrdtOperation> opsA = List.of(opA1, opA2);
        CrdtEngine.applyBatch(serverNote, opsA);

        // Sync Device B -> Server
        List<CrdtOperation> opsB = List.of(opB1, opB2, opB3);
        CrdtEngine.applyBatch(serverNote, opsB);

        // Devices pull updates from server
        CrdtNote convergedA = CrdtEngine.merge(deviceANote, serverNote);
        CrdtNote convergedB = CrdtEngine.merge(deviceBNote, serverNote);

        // --- VERIFY CONVERGENCE & CONFLICT-FREE GUARANTEES ---
        // 1. Both devices now have all 3 blocks (No data was lost!)
        assertEquals(3, convergedA.getActiveBlocks().size(), "Device A should have all 3 blocks");
        assertEquals(3, convergedB.getActiveBlocks().size(), "Device B should have all 3 blocks");

        // 2. Both devices have both tags (Observed-Remove Set merged cleanly)
        assertTrue(convergedA.getActiveTags().contains("BFS"));
        assertTrue(convergedA.getActiveTags().contains("DFS"));
        assertTrue(convergedB.getActiveTags().contains("BFS"));
        assertTrue(convergedB.getActiveTags().contains("DFS"));

        // 3. Both devices have the updated title
        assertEquals("Graph Traversal & Search Algorithms", convergedA.getTitle());
        assertEquals("Graph Traversal & Search Algorithms", convergedB.getTitle());

        // 4. Block order is 100% identical on both devices
        List<CrdtBlock> blocksA = convergedA.getActiveBlocks();
        List<CrdtBlock> blocksB = convergedB.getActiveBlocks();
        for (int i = 0; i < blocksA.size(); i++) {
            assertEquals(blocksA.get(i).getBlockId(), blocksB.get(i).getBlockId(), "Block at position " + i + " must match");
            assertEquals(blocksA.get(i).getContent(), blocksB.get(i).getContent());
        }

        // 5. Commutativity: merge(A, B) == merge(B, A)
        CrdtNote directMergeAB = CrdtEngine.merge(deviceANote, deviceBNote);
        CrdtNote directMergeBA = CrdtEngine.merge(deviceBNote, deviceANote);
        assertEquals(directMergeAB.getActiveBlocks().size(), directMergeBA.getActiveBlocks().size());
        assertEquals(directMergeAB.getTitle(), directMergeBA.getTitle());
        assertEquals(directMergeAB.getActiveTags(), directMergeBA.getActiveTags());
    }

    @Test
    @DisplayName("Monotonic lesson progress merges correctly without regressions")
    void testLessonProgressConvergence() {
        String lessonId = "lesson-operating-systems-threads";
        String studentId = "student-alice";

        CrdtProgress progressPhone = new CrdtProgress(lessonId, studentId, 45, false, 70, "b-mutex", 10, "phone", 1000);
        CrdtProgress progressTablet = new CrdtProgress(lessonId, studentId, 85, true, 95, "b-semaphore", 15, "tablet", 1200);

        // Merge phone into tablet
        progressPhone.merge(progressTablet);

        // Progress percentage should be max(45, 85) = 85
        assertEquals(85, progressPhone.getProgressPercentage());
        // Completed must be true because tablet completed it
        assertTrue(progressPhone.isCompleted());
        // Quiz score should be highest score = 95
        assertEquals(95, progressPhone.getQuizScore());
        // Last read block should be from higher lamport clock = "b-semaphore"
        assertEquals("b-semaphore", progressPhone.getLastReadBlockId());
    }
}
