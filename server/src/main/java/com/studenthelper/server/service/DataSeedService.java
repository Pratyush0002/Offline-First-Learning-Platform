package com.studenthelper.server.service;

import com.studenthelper.crdt.clock.LamportClock;
import com.studenthelper.crdt.model.BlockType;
import com.studenthelper.crdt.model.FractionalIndex;
import com.studenthelper.crdt.operation.CrdtOperation;
import com.studenthelper.server.dto.SyncRequest;
import com.studenthelper.server.entity.CourseEntity;
import com.studenthelper.server.entity.LessonEntity;
import com.studenthelper.server.repository.CourseRepository;
import com.studenthelper.server.repository.LessonRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataSeedService {
    private static final Logger log = LoggerFactory.getLogger(DataSeedService.class);

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final SyncService syncService;

    public DataSeedService(CourseRepository courseRepository, LessonRepository lessonRepository, SyncService syncService) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.syncService = syncService;
    }

    @PostConstruct
    public void seedInitialData() {
        if (courseRepository.count() > 0) {
            log.info("Database already seeded with courses and lessons.");
            return;
        }

        log.info("Seeding initial courses, lessons, and sample CRDT notes into SQLite database...");

        // 1. Data Structures & Algorithms
        CourseEntity dsaCourse = new CourseEntity(
                "course-dsa-101",
                "Data Structures & Algorithms",
                "Master core data structures and algorithms with offline interactive lessons and practice notes.",
                "Computer Science",
                "Intermediate",
                "/assets/dsa_cover.png",
                12
        );
        courseRepository.save(dsaCourse);

        LessonEntity l1 = new LessonEntity(
                "lesson-dsa-big-o",
                dsaCourse.getId(),
                "Big-O Notation & Complexity Analysis",
                1,
                "Understand asymptotic runtime analysis, worst-case, best-case, and average-case performance.",
                """
                # Big-O Notation & Complexity Analysis
                
                In computer science, **asymptotic analysis** evaluates how the running time and memory consumption of an algorithm scale with input size $N$.
                
                ## Key Asymptotic Notations:
                1. **Big-O ($O(g(n))$)**: Represents the tight mathematical *upper bound* (worst-case performance).
                2. **Big-Omega ($\\\\Omega(g(n))$)**: Represents the mathematical *lower bound* (best-case performance).
                3. **Big-Theta ($\\\\Theta(g(n))$)**: Represents an asymptotically tight bound where upper and lower bounds match.
                
                ## Common Complexity Classes
                - $O(1)$: Constant Time (Hash map lookup)
                - $O(\\\\log N)$: Logarithmic Time (Binary search)
                - $O(N)$: Linear Time (Array scan)
                - $O(N \\\\log N)$: Linearithmic Time (Merge Sort, Quick Sort average)
                - $O(N^2)$: Quadratic Time (Nested loops, Bubble Sort)
                - $O(2^N)$: Exponential Time (Recursive subsets, Fibonacci naive)
                
                ```java
                // Binary Search - O(log N) Time, O(1) Space
                public int binarySearch(int[] arr, int target) {
                    int low = 0, high = arr.length - 1;
                    while (low <= high) {
                        int mid = low + (high - low) / 2;
                        if (arr[mid] == target) return mid;
                        else if (arr[mid] < target) low = mid + 1;
                        else high = mid - 1;
                    }
                    return -1;
                }
                ```
                """,
                25,
                """
                [
                  {"question": "What is the worst-case time complexity of Binary Search on a sorted array of size N?", "options": ["O(N)", "O(log N)", "O(1)", "O(N^2)"], "correctIndex": 1},
                  {"question": "Which sorting algorithm guarantees O(N log N) worst-case time complexity?", "options": ["Quick Sort", "Bubble Sort", "Merge Sort", "Insertion Sort"], "correctIndex": 2}
                ]
                """,
                "{\"summary\": \"Cheat sheet for common Big-O complexities and array operations\"}"
        );

        LessonEntity l2 = new LessonEntity(
                "lesson-dsa-graphs",
                dsaCourse.getId(),
                "Graph Traversal: BFS & DFS with State Tracking",
                2,
                "Explore Breadth-First Search (Queue-based) and Depth-First Search (Stack-based) with adjacency lists.",
                """
                # Graph Traversal: BFS & DFS
                
                Graphs represent pairwise relationships between vertices $V$ connected by edges $E$.
                
                ## 1. Breadth-First Search (BFS)
                - Explores the graph layer by layer (closest neighbors first).
                - Uses a **FIFO Queue**.
                - Optimal for finding the shortest path on unweighted graphs.
                - Time Complexity: $O(V + E)$
                
                ```kotlin
                fun bfs(startNode: Int, adjList: Map<Int, List<Int>>) {
                    val visited = mutableSetOf<Int>()
                    val queue = ArrayDeque<Int>()
                    
                    visited.add(startNode)
                    queue.add(startNode)
                    
                    while (queue.isNotEmpty()) {
                        val current = queue.removeFirst()
                        println("Visited node: $current")
                        
                        for (neighbor in adjList[current] ?: emptyList()) {
                            if (neighbor !in visited) {
                                visited.add(neighbor)
                                queue.add(neighbor)
                            }
                        }
                    }
                }
                ```
                
                ## 2. Depth-First Search (DFS)
                - Explores along each branch as deeply as possible before backtracking.
                - Uses a **LIFO Stack** or call stack recursion.
                - Optimal for cycle detection, topological sorting, and maze exploration.
                """,
                35,
                """
                [
                  {"question": "Which data structure is fundamentally utilized by Breadth-First Search?", "options": ["Stack", "Queue", "Binary Heap", "Priority Queue"], "correctIndex": 1},
                  {"question": "What is the time complexity of BFS on an adjacency list representation with V vertices and E edges?", "options": ["O(V * E)", "O(V + E)", "O(V^2)", "O(E log V)"], "correctIndex": 1}
                ]
                """,
                "{\"algorithmVisualization\": \"BFS vs DFS exploration order\"}"
        );

        lessonRepository.saveAll(List.of(l1, l2));

        // 2. Offline-First & Distributed Systems Course
        CourseEntity distCourse = new CourseEntity(
                "course-sys-201",
                "Offline-First & Distributed Systems",
                "Learn how to build resilient applications that thrive with intermittent or zero internet connectivity.",
                "Systems Engineering",
                "Advanced",
                "/assets/crdt_cover.png",
                16
        );
        courseRepository.save(distCourse);

        LessonEntity l3 = new LessonEntity(
                "lesson-crdt-intro",
                distCourse.getId(),
                "Conflict-Free Replicated Data Types (CRDTs)",
                1,
                "Discover how CRDTs allow multiple devices to edit data offline and converge without merge conflicts.",
                """
                # Conflict-Free Replicated Data Types (CRDTs)
                
                In an offline-first learning platform, students read, take notes, and complete exercises without guaranteed network connectivity.
                
                ## The Multi-Device Dilemma
                If a student edits a study note on their tablet on the bus (offline) and simultaneously edits the same note on their phone at home (offline), both devices modify their local SQLite databases.
                
                Under traditional REST architectures:
                - **Last-Write-Wins (LWW)** silently deletes the tablet's edits when the phone uploads later!
                - **Git-style merge conflicts** force students to resolve diffs, creating high friction.
                
                ## CRDT Mathematical Foundations
                A state-based CRDT defines a join semi-lattice $(S, \\\\sqcup)$ with a partial order $\\\\le$ such that:
                1. **Commutative**: $A \\\\sqcup B = B \\\\sqcup A$ (order of arrival does not matter).
                2. **Associative**: $(A \\\\sqcup B) \\\\sqcup C = A \\\\sqcup (B \\\\sqcup C)$ (grouping does not matter).
                3. **Idempotent**: $A \\\\sqcup A = A$ (duplicate network deliveries are harmless).
                
                ## Sequence CRDTs and Fractional Indexing
                Each note block is assigned a fractional index position key. Inserting between blocks generates an intermediate key with client tie-breakers, guaranteeing zero collision and 100% deterministic ordering across all devices!
                """,
                40,
                """
                [
                  {"question": "What three mathematical properties guarantee that CRDT replicas will deterministically converge?", "options": ["Reflexive, Symmetric, Transitive", "Commutative, Associative, Idempotent", "Deterministic, Reversible, Isolated", "Atomicity, Consistency, Durability"], "correctIndex": 1},
                  {"question": "How does Fractional Indexing resolve concurrent insertions between the same two blocks?", "options": ["By rejecting the second insertion", "By generating an intermediate key with client-id tiebreaker", "By overwriting the first block", "By asking the user to manually pick"], "correctIndex": 1}
                ]
                """,
                "{\"references\": [\"Marc Shapiro et al., A comprehensive study of Convergent and Commutative Replicated Data Types, Inria\"]}"
        );

        LessonEntity l4 = new LessonEntity(
                "lesson-android-room-workmanager",
                distCourse.getId(),
                "Offline-First Android: Room SQLite & WorkManager",
                2,
                "Architecting Android apps with local Room cache, NetworkCallback monitoring, and WorkManager sync.",
                """
                # Offline-First Android Architecture
                
                An offline-first Android application treats the local database as the **Single Source of Truth (SSOT)**.
                
                ## Key Architectural Pillars:
                1. **Room SQLite**: All UI queries read directly from Room tables via `StateFlow` or `LiveData`.
                2. **Mutation Outbox Queue**: When a user creates or edits a note, a `CrdtOperation` is recorded in Room.
                3. **WorkManager Constraint-Based Sync**: `SyncWorker` is registered with:
                   ```kotlin
                   val constraints = Constraints.Builder()
                       .setRequiredNetworkType(NetworkType.CONNECTED)
                       .build()
                   ```
                4. When connectivity returns, WorkManager activates in the background, pushes pending mutations to Spring Boot, pulls new deltas, and updates Room seamlessly!
                """,
                30,
                """
                [
                  {"question": "Which Jetpack component is best suited for guaranteed background synchronization when network reconnects?", "options": ["AlarmManager", "WorkManager", "HandlerThread", "JobIntentService"], "correctIndex": 1}
                ]
                """,
                "{\"docs\": \"Android Jetpack Room and WorkManager Official Guides\"}"
        );

        lessonRepository.saveAll(List.of(l3, l4));

        // 3. Seed an initial sample study note with real CRDT operations
        seedSampleNote();
        log.info("Database seeding successfully completed.");
    }

    private void seedSampleNote() {
        String noteId = "note-welcome-guide";
        String lessonId = "lesson-crdt-intro";
        String authorId = "student-demo";
        LamportClock clock = new LamportClock(1);

        List<CrdtOperation> ops = new ArrayList<>();

        // Create Note
        ops.add(CrdtOperation.createNote(noteId, lessonId, "My Offline Study Notes: CRDTs & Distributed Systems", "server", clock.tick()));

        // Block 1: Heading
        FractionalIndex pos1 = FractionalIndex.initial("server", clock.tick());
        ops.add(CrdtOperation.insertBlock(noteId, "b-head-1", pos1, BlockType.HEADING_1, "Core Insights on Offline-First Sync", "server", clock.tick()));

        // Block 2: Key formula / callout
        FractionalIndex pos2 = FractionalIndex.between(pos1, null, "server", clock.tick());
        ops.add(CrdtOperation.insertBlock(noteId, "b-callout-1", pos2, BlockType.CALLOUT_NOTE, "Always treat the local SQLite database on Android as the single source of truth for snappy UI response.", "server", clock.tick()));

        // Block 3: Bullet points
        FractionalIndex pos3 = FractionalIndex.between(pos2, null, "server", clock.tick());
        ops.add(CrdtOperation.insertBlock(noteId, "b-bullet-1", pos3, BlockType.BULLET_ITEM, "Operations are immutable and causal: (lamportClock, clientId, opId).", "server", clock.tick()));

        FractionalIndex pos4 = FractionalIndex.between(pos3, null, "server", clock.tick());
        ops.add(CrdtOperation.insertBlock(noteId, "b-bullet-2", pos4, BlockType.BULLET_ITEM, "Fractional indexing prevents list index shifts across concurrent edits.", "server", clock.tick()));

        // Block 4: Todo Checklist
        FractionalIndex pos5 = FractionalIndex.between(pos4, null, "server", clock.tick());
        ops.add(CrdtOperation.insertBlock(noteId, "b-todo-1", pos5, BlockType.TODO_ITEM, "Read Chapter 1 on Asymptotic Notation", "server", clock.tick()));
        ops.add(CrdtOperation.toggleBlockCheck(noteId, "b-todo-1", true, "server", clock.tick()));

        FractionalIndex pos6 = FractionalIndex.between(pos5, null, "server", clock.tick());
        ops.add(CrdtOperation.insertBlock(noteId, "b-todo-2", pos6, BlockType.TODO_ITEM, "Simulate multi-device offline sync in the web dashboard", "server", clock.tick()));

        // Tags
        ops.add(CrdtOperation.addTag(noteId, "CRDT", "server", clock.tick()));
        ops.add(CrdtOperation.addTag(noteId, "DistributedSystems", "server", clock.tick()));
        ops.add(CrdtOperation.addTag(noteId, "Android", "server", clock.tick()));

        // Submit via SyncService
        SyncRequest seedRequest = new SyncRequest("server", "Spring Boot Server", null, ops);
        syncService.syncDelta(seedRequest);
    }
}
