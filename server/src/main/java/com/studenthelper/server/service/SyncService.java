package com.studenthelper.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studenthelper.crdt.clock.VersionVector;
import com.studenthelper.crdt.engine.CrdtEngine;
import com.studenthelper.crdt.model.CrdtNote;
import com.studenthelper.crdt.model.CrdtProgress;
import com.studenthelper.crdt.operation.CrdtOperation;
import com.studenthelper.crdt.operation.OperationType;
import com.studenthelper.server.dto.SyncRequest;
import com.studenthelper.server.dto.SyncResponse;
import com.studenthelper.server.entity.CrdtOperationEntity;
import com.studenthelper.server.entity.DeviceSyncEntity;
import com.studenthelper.server.entity.LessonProgressEntity;
import com.studenthelper.server.entity.NoteEntity;
import com.studenthelper.server.repository.CrdtOperationRepository;
import com.studenthelper.server.repository.DeviceSyncRepository;
import com.studenthelper.server.repository.LessonProgressRepository;
import com.studenthelper.server.repository.NoteEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SyncService {
    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final CrdtOperationRepository operationRepository;
    private final NoteEntityRepository noteRepository;
    private final LessonProgressRepository progressRepository;
    private final DeviceSyncRepository deviceSyncRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public SyncService(
            CrdtOperationRepository operationRepository,
            NoteEntityRepository noteRepository,
            LessonProgressRepository progressRepository,
            DeviceSyncRepository deviceSyncRepository,
            ObjectMapper objectMapper,
            Optional<SimpMessagingTemplate> messagingTemplate) {
        this.operationRepository = operationRepository;
        this.noteRepository = noteRepository;
        this.progressRepository = progressRepository;
        this.deviceSyncRepository = deviceSyncRepository;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate.orElse(null);
    }

    /**
     * Primary bidirectional delta sync endpoint:
     * 1. Ingests client operations and applies them to authoritative SQLite state.
     * 2. Finds server operations that client has not yet observed.
     * 3. Updates device vector clocks.
     * 4. Returns acknowledged ops + missing ops + server vector clock.
     */
    @Transactional
    public SyncResponse syncDelta(SyncRequest request) {
        String clientId = request.getClientId() != null ? request.getClientId() : "unknown-device";
        VersionVector clientVector = request.getClientVector() != null ? request.getClientVector() : new VersionVector();
        List<CrdtOperation> clientOps = request.getOperations() != null ? request.getOperations() : Collections.emptyList();

        log.info("Processing syncDelta from client '{}' with {} incoming operations", clientId, clientOps.size());

        List<String> ackOpIds = new ArrayList<>();
        Set<String> affectedNotes = new HashSet<>();
        long maxClockSeen = clientVector.getClock(clientId);

        // 1. Process and save incoming client operations
        for (CrdtOperation op : clientOps) {
            maxClockSeen = Math.max(maxClockSeen, op.getLamportClock());

            if (!operationRepository.existsById(op.getOpId())) {
                CrdtOperationEntity entity = new CrdtOperationEntity(
                        op.getOpId(),
                        op.getNoteId(),
                        op.getType().name(),
                        op.getClientId(),
                        op.getLamportClock(),
                        op.getTimestamp(),
                        op.getTargetId(),
                        op.getPayload(),
                        op.getSecondaryPayload()
                );
                operationRepository.save(entity);
            }
            ackOpIds.add(op.getOpId());

            if (op.getType() == OperationType.UPDATE_PROGRESS) {
                applyProgressOperation(op);
            } else {
                affectedNotes.add(op.getNoteId());
            }
        }

        // 2. Re-apply CRDT state for affected notes and persist updated note snapshots
        List<CrdtNote> updatedNoteSnapshots = new ArrayList<>();
        for (String noteId : affectedNotes) {
            CrdtNote convergedNote = rebuildNoteFromOperations(noteId);
            if (convergedNote != null) {
                saveNoteSnapshot(convergedNote);
                updatedNoteSnapshots.add(convergedNote);
            }
        }

        // 3. Update device sync state
        deviceSyncRepository.save(new DeviceSyncEntity(
                clientId,
                request.getDeviceName(),
                System.currentTimeMillis(),
                maxClockSeen
        ));

        // 4. Compute current Server Version Vector
        VersionVector serverVector = computeServerVersionVector();

        // 5. Find operations that the client is missing
        List<CrdtOperation> missingOperations = findOperationsMissingForClient(clientVector, clientId);

        log.info("Sync complete for client '{}': Acknowledged {}, Returning {} missing operations",
                clientId, ackOpIds.size(), missingOperations.size());

        // 6. Broadcast to connected WebSocket clients (if any)
        if (messagingTemplate != null && !clientOps.isEmpty()) {
            try {
                messagingTemplate.convertAndSend("/topic/sync", Map.of(
                        "sender", clientId,
                        "opCount", clientOps.size(),
                        "affectedNotes", affectedNotes
                ));
            } catch (Exception e) {
                log.warn("Failed to broadcast WebSocket sync event: {}", e.getMessage());
            }
        }

        SyncResponse response = new SyncResponse("SUCCESS", ackOpIds, missingOperations, serverVector);
        response.setUpdatedNotes(updatedNoteSnapshots);
        return response;
    }

    /**
     * Finds all operations in the server log that the client has not yet seen,
     * excluding operations originating from the client itself.
     */
    public List<CrdtOperation> findOperationsMissingForClient(VersionVector clientVector, String requestingClientId) {
        List<CrdtOperationEntity> allEntities = operationRepository.findAllByOrderByLamportClockAscClientIdAsc();
        List<CrdtOperation> missing = new ArrayList<>();

        for (CrdtOperationEntity entity : allEntities) {
            long clientKnownClock = clientVector.getClock(entity.getClientId());
            if (entity.getLamportClock() > clientKnownClock) {
                missing.add(toCrdtOperation(entity));
            }
        }
        return missing;
    }

    /**
     * Rebuilds the authoritative CRDT state of a note by replaying all historical operations
     * in total causal order (Lamport clock, clientId, opId).
     */
    public CrdtNote rebuildNoteFromOperations(String noteId) {
        List<CrdtOperationEntity> entities = operationRepository.findByNoteIdOrderByLamportClockAscClientIdAsc(noteId);
        if (entities.isEmpty()) {
            Optional<NoteEntity> existing = noteRepository.findById(noteId);
            if (existing.isPresent() && existing.get().getCrdtStateJson() != null) {
                try {
                    return objectMapper.readValue(existing.get().getCrdtStateJson(), CrdtNote.class);
                } catch (Exception e) {
                    log.error("Failed to parse cached CRDT state: {}", e.getMessage());
                }
            }
            return null;
        }

        List<CrdtOperation> ops = entities.stream()
                .map(this::toCrdtOperation)
                .sorted()
                .toList();

        // Determine base note parameters
        String lessonId = null;
        String title = "Untitled Note";
        String initialClient = "server";
        long initialClock = 1;

        for (CrdtOperation op : ops) {
            if (op.getType() == OperationType.CREATE_NOTE) {
                lessonId = op.getTargetId();
                if (op.getPayload() != null) title = op.getPayload();
                initialClient = op.getClientId();
                initialClock = op.getLamportClock();
                break;
            }
        }

        CrdtNote note = new CrdtNote(noteId, lessonId, "student", title, initialClient, initialClock);
        CrdtEngine.applyBatch(note, ops);
        return note;
    }

    /**
     * Persists the reconstructed CrdtNote snapshot into the SQLite notes table.
     */
    private void saveNoteSnapshot(CrdtNote note) {
        try {
            String stateJson = objectMapper.writeValueAsString(note);
            String tagsJson = objectMapper.writeValueAsString(note.getActiveTags());

            NoteEntity entity = new NoteEntity(
                    note.getId(),
                    note.getLessonId(),
                    note.getAuthorId(),
                    note.getTitle(),
                    note.getTitleClock(),
                    note.getTitleClientId(),
                    note.toMarkdown(),
                    note.isDeleted(),
                    note.getDeleteClock(),
                    tagsJson,
                    stateJson,
                    note.getCreatedAt(),
                    note.getUpdatedAt()
            );
            noteRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to serialize CrdtNote for persistence: {}", e.getMessage());
        }
    }

    private void applyProgressOperation(CrdtOperation op) {
        String lessonId = op.getTargetId();
        String studentId = op.getClientId();
        String payload = op.getPayload(); // "progressPercent:completed:quizScore"

        int progressPercent = 0;
        boolean completed = false;
        int quizScore = 0;

        if (payload != null) {
            String[] parts = payload.split(":");
            if (parts.length > 0) progressPercent = Integer.parseInt(parts[0]);
            if (parts.length > 1) completed = Boolean.parseBoolean(parts[1]);
            if (parts.length > 2) quizScore = Integer.parseInt(parts[2]);
        }

        CrdtProgress incoming = new CrdtProgress(lessonId, studentId, progressPercent, completed, quizScore, null, op.getLamportClock(), op.getClientId(), op.getTimestamp());

        Optional<LessonProgressEntity> existingOpt = progressRepository.findByStudentIdAndLessonId(studentId, lessonId);
        if (existingOpt.isPresent()) {
            LessonProgressEntity existing = existingOpt.get();
            CrdtProgress current = new CrdtProgress(
                    existing.getLessonId(),
                    existing.getStudentId(),
                    existing.getProgressPercentage(),
                    existing.isCompleted(),
                    existing.getQuizScore(),
                    existing.getLastReadBlockId(),
                    existing.getLamportClock(),
                    existing.getClientId(),
                    existing.getUpdatedAt()
            );
            current.merge(incoming);

            existing.setProgressPercentage(current.getProgressPercentage());
            existing.setCompleted(current.isCompleted());
            existing.setQuizScore(current.getQuizScore());
            existing.setLamportClock(current.getLamportClock());
            existing.setClientId(current.getClientId());
            existing.setUpdatedAt(current.getLastUpdated());
            progressRepository.save(existing);
        } else {
            LessonProgressEntity newEntity = new LessonProgressEntity(
                    studentId,
                    lessonId,
                    incoming.getProgressPercentage(),
                    incoming.isCompleted(),
                    incoming.getQuizScore(),
                    null,
                    incoming.getLamportClock(),
                    incoming.getClientId()
            );
            progressRepository.save(newEntity);
        }
    }

    public VersionVector computeServerVersionVector() {
        VersionVector vector = new VersionVector();
        List<CrdtOperationEntity> allOps = operationRepository.findAll();
        for (CrdtOperationEntity op : allOps) {
            vector.updateMax(op.getClientId(), op.getLamportClock());
        }
        return vector;
    }

    private CrdtOperation toCrdtOperation(CrdtOperationEntity entity) {
        return new CrdtOperation(
                entity.getOpId(),
                entity.getNoteId(),
                OperationType.valueOf(entity.getType()),
                entity.getClientId(),
                entity.getLamportClock(),
                entity.getTimestamp(),
                entity.getTargetId(),
                entity.getPayload(),
                entity.getSecondaryPayload()
        );
    }
}
