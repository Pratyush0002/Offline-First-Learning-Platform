package com.studenthelper.server.controller;

import com.studenthelper.crdt.clock.VersionVector;
import com.studenthelper.server.dto.SyncRequest;
import com.studenthelper.server.dto.SyncResponse;
import com.studenthelper.server.entity.CrdtOperationEntity;
import com.studenthelper.server.repository.CrdtOperationRepository;
import com.studenthelper.server.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    private final SyncService syncService;
    private final CrdtOperationRepository operationRepository;

    public SyncController(SyncService syncService, CrdtOperationRepository operationRepository) {
        this.syncService = syncService;
        this.operationRepository = operationRepository;
    }

    /**
     * Primary bidirectional sync endpoint for Android and web clients.
     */
    @PostMapping("/delta")
    public ResponseEntity<SyncResponse> syncDelta(@RequestBody SyncRequest request) {
        return ResponseEntity.ok(syncService.syncDelta(request));
    }

    /**
     * Retrieves the current VersionVector of the server.
     */
    @GetMapping("/vector")
    public ResponseEntity<VersionVector> getServerVersionVector() {
        return ResponseEntity.ok(syncService.computeServerVersionVector());
    }

    /**
     * Inspects the raw CRDT operation log.
     */
    @GetMapping("/operations")
    public ResponseEntity<List<CrdtOperationEntity>> getAllOperations() {
        return ResponseEntity.ok(operationRepository.findAllByOrderByLamportClockAscClientIdAsc());
    }
}
