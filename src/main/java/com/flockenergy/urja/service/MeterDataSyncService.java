package com.flockenergy.urja.service;

import com.flockenergy.urja.client.UrjaPortalClient;
import com.flockenergy.urja.config.UrjaPortalProperties;
import com.flockenergy.urja.dto.*;
import com.flockenergy.urja.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MeterDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(MeterDataSyncService.class);

    private final UrjaPortalClient portalClient;
    private final UrjaPortalProperties properties;

    private final Map<String, Meter> meterById = new ConcurrentHashMap<>();
    private final Map<String, Meter> meterBySerial = new ConcurrentHashMap<>();
    private final Map<String, List<Meter>> metersByDtCode = new ConcurrentHashMap<>();
    private final Map<String, Transformer> transformerByCode = new ConcurrentHashMap<>();
    private final List<Meter> allMeters = new CopyOnWriteArrayList<>();
    private final List<Transformer> allTransformers = new CopyOnWriteArrayList<>();

    private volatile boolean synced = false;
    private volatile Instant lastSyncTime = null;
    private volatile String syncStatus = "INITIALIZING";

    public record SyncResult(boolean success, int metersSynced, int transformersSynced, Instant timestamp, String message) {}

    public MeterDataSyncService(UrjaPortalClient portalClient, UrjaPortalProperties properties) {
        this.portalClient = portalClient;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (properties.isSyncOnStartup()) {
            log.info("Starting initial synchronization with Urja Meter Ops portal...");
            new Thread(this::syncNow, "Urja-Startup-Sync").start();
        }
    }

    @Scheduled(cron = "${urja.portal.sync-cron:0 */15 * * * *}")
    public void scheduledSync() {
        log.info("Executing scheduled sync with Urja portal...");
        syncNow();
    }

    public synchronized SyncResult syncNow() {
        this.syncStatus = "SYNCING";
        log.info("Triggering Urja Portal full data synchronization...");
        try {
            // 1. Fetch Transformers (pages 1 and 2)
            List<RawDtRecord> rawDts = new ArrayList<>();
            RawDtsResponse p1 = portalClient.getTransformers(1);
            if (p1 != null && p1.data() != null) rawDts.addAll(p1.data());
            RawDtsResponse p2 = portalClient.getTransformers(2);
            if (p2 != null && p2.data() != null) rawDts.addAll(p2.data());

            // 2. Fetch Full Bulk Export of all meters using HMAC signing
            RawExportResponse exportResponse = portalClient.exportAllMeters();
            if (exportResponse == null || exportResponse.data() == null) {
                this.syncStatus = "FAILED";
                return new SyncResult(false, 0, 0, Instant.now(), "Export response was null or empty");
            }

            List<Meter> parsedMeters = new ArrayList<>();
            Map<String, Meter> newMeterById = new HashMap<>();
            Map<String, Meter> newMeterBySerial = new HashMap<>();
            Map<String, List<Meter>> newMetersByDtCode = new HashMap<>();

            for (RawExportMeter raw : exportResponse.data()) {
                Meter meter = mapToMeter(raw);
                parsedMeters.add(meter);
                newMeterById.put(meter.meterId(), meter);
                if (meter.serialNo() != null) {
                    newMeterBySerial.put(meter.serialNo().toUpperCase(), meter);
                }
                if (meter.dtCode() != null) {
                    newMetersByDtCode.computeIfAbsent(meter.dtCode(), k -> new ArrayList<>()).add(meter);
                }
            }

            // 3. Build Transformers with aggregated meter metrics
            List<Transformer> parsedTransformers = new ArrayList<>();
            Map<String, Transformer> newTransformerByCode = new HashMap<>();

            for (RawDtRecord rawDt : rawDts) {
                List<Meter> connected = newMetersByDtCode.getOrDefault(rawDt.code(), List.of());
                int installed = 0;
                int faulty = 0;
                int decommissioned = 0;

                for (Meter m : connected) {
                    if ("Installed".equalsIgnoreCase(m.installStatus())) installed++;
                    else if ("Faulty".equalsIgnoreCase(m.installStatus())) faulty++;
                    else if ("Decommissioned".equalsIgnoreCase(m.installStatus())) decommissioned++;
                }

                String status = "NORMAL";
                if (faulty > 0) status = "ATTENTION_REQUIRED";

                Transformer t = new Transformer(
                        rawDt.code(),
                        rawDt.name(),
                        rawDt.feederCode(),
                        rawDt.capacityKva(),
                        connected.size(),
                        installed,
                        faulty,
                        decommissioned,
                        status
                );
                parsedTransformers.add(t);
                newTransformerByCode.put(t.code(), t);
            }

            // Atomic-like update
            meterById.clear();
            meterById.putAll(newMeterById);

            meterBySerial.clear();
            meterBySerial.putAll(newMeterBySerial);

            metersByDtCode.clear();
            metersByDtCode.putAll(newMetersByDtCode);

            transformerByCode.clear();
            transformerByCode.putAll(newTransformerByCode);

            allMeters.clear();
            allMeters.addAll(parsedMeters);

            allTransformers.clear();
            allTransformers.addAll(parsedTransformers);

            this.synced = true;
            this.lastSyncTime = Instant.now();
            this.syncStatus = "SYNCHRONIZED";

            log.info("Urja Portal sync completed successfully. Indexed {} meters and {} transformers.",
                    allMeters.size(), allTransformers.size());

            return new SyncResult(true, allMeters.size(), allTransformers.size(), lastSyncTime, "Sync successful");

        } catch (Exception e) {
            log.error("Failed to synchronize with Urja portal: {}", e.getMessage(), e);
            this.syncStatus = "ERROR: " + e.getMessage();
            return new SyncResult(false, allMeters.size(), allTransformers.size(), Instant.now(), "Sync failed: " + e.getMessage());
        }
    }

    private Meter mapToMeter(RawExportMeter raw) {
        GeoLocation geo = null;
        if (raw.geo() != null && raw.geo().lat() != null && raw.geo().lng() != null) {
            geo = new GeoLocation(raw.geo().lat(), raw.geo().lng());
        }

        MeterHierarchy hierarchy = null;
        if (raw.hierarchy() != null) {
            RawMeterHierarchy h = raw.hierarchy();
            hierarchy = new MeterHierarchy(
                    h.zone() != null ? new HierarchyLevel(h.zone().name(), h.zone().code()) : null,
                    h.circle() != null ? new HierarchyLevel(h.circle().name(), h.circle().code()) : null,
                    h.division() != null ? new HierarchyLevel(h.division().name(), h.division().code()) : null,
                    h.subdivision() != null ? new HierarchyLevel(h.subdivision().name(), h.subdivision().code()) : null,
                    h.substation() != null ? new HierarchyLevel(h.substation().name(), h.substation().code()) : null,
                    h.feeder() != null ? new HierarchyLevel(h.feeder().name(), h.feeder().code()) : null,
                    h.dt() != null ? new HierarchyLevel(h.dt().name(), h.dt().code()) : null
            );
        }

        return new Meter(
                raw.meterId(),
                raw.serialNo(),
                raw.make(),
                raw.phaseType(),
                raw.installStatus(),
                raw.installType(),
                raw.build(),
                raw.dtCode(),
                geo,
                hierarchy
        );
    }

    public boolean isSynced() {
        return synced;
    }

    public Instant getLastSyncTime() {
        return lastSyncTime;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public List<Meter> getAllMeters() {
        return Collections.unmodifiableList(allMeters);
    }

    public Optional<Meter> findMeterById(String meterId) {
        if (meterId == null) return Optional.empty();
        return Optional.ofNullable(meterById.get(meterId));
    }

    public Optional<Meter> findMeterBySerial(String serialNo) {
        if (serialNo == null) return Optional.empty();
        return Optional.ofNullable(meterBySerial.get(serialNo.toUpperCase()));
    }

    public List<Meter> findMetersByDtCode(String dtCode) {
        if (dtCode == null) return List.of();
        return metersByDtCode.getOrDefault(dtCode, List.of());
    }

    public List<Transformer> getAllTransformers() {
        return Collections.unmodifiableList(allTransformers);
    }

    public Optional<Transformer> findTransformerByCode(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(transformerByCode.get(code));
    }
}
