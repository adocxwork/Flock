package com.flockenergy.urja.controller;

import com.flockenergy.urja.service.MeterDataSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/sync")
@Tag(name = "Dataset Sync", description = "Manage dataset freshness, trigger full synchronization, and check sync status")
public class SyncController {

    private final MeterDataSyncService syncService;

    public SyncController(MeterDataSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping
    @Operation(
            summary = "Trigger full dataset sync",
            description = "Triggers an on-demand synchronization cycle against the upstream Urja portal using HMAC-signed bulk export, updating all in-memory indexes."
    )
    public ResponseEntity<MeterDataSyncService.SyncResult> triggerSync() {
        return ResponseEntity.ok(syncService.syncNow());
    }

    @GetMapping("/status")
    @Operation(
            summary = "Check sync status and freshness",
            description = "Reports the current synchronization state, timestamp of last successful sync, and record counts."
    )
    public ResponseEntity<SyncStatusResponse> getSyncStatus() {
        return ResponseEntity.ok(new SyncStatusResponse(
                syncService.isSynced(),
                syncService.getSyncStatus(),
                syncService.getLastSyncTime(),
                syncService.getAllMeters().size(),
                syncService.getAllTransformers().size()
        ));
    }

    public record SyncStatusResponse(
            boolean isSynced,
            String status,
            Instant lastSyncTime,
            int totalMetersIndexed,
            int totalTransformersIndexed
    ) {}
}
