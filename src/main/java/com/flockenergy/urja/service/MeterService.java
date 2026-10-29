package com.flockenergy.urja.service;

import com.flockenergy.urja.client.UrjaPortalClient;
import com.flockenergy.urja.dto.RawEnergyRecord;
import com.flockenergy.urja.dto.RawEnergyResponse;
import com.flockenergy.urja.dto.RawGeoResponse;
import com.flockenergy.urja.dto.RawMeterSearchResponse;
import com.flockenergy.urja.exception.ResourceNotFoundException;
import com.flockenergy.urja.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeterService {

    private static final Logger log = LoggerFactory.getLogger(MeterService.class);

    private final MeterDataSyncService syncService;
    private final UrjaPortalClient portalClient;

    public MeterService(MeterDataSyncService syncService, UrjaPortalClient portalClient) {
        this.syncService = syncService;
        this.portalClient = portalClient;
    }

    public PagedResponse<Meter> searchAndFilterMeters(
            String query,
            String make,
            String phase,
            String status,
            String dtCode,
            Double nearLat,
            Double nearLng,
            Double radiusKm,
            int page,
            int size
    ) {
        // Fast path: use synchronized in-memory index
        if (syncService.isSynced()) {
            List<Meter> filtered = syncService.getAllMeters().stream()
                    .filter(m -> matchesText(m, query))
                    .filter(m -> matchesMake(m, make))
                    .filter(m -> matchesPhase(m, phase))
                    .filter(m -> matchesStatus(m, status))
                    .filter(m -> matchesDt(m, dtCode))
                    .filter(m -> matchesRadius(m, nearLat, nearLng, radiusKm))
                    .collect(Collectors.toList());

            // If geo-search requested, sort by proximity to the target coordinate
            if (nearLat != null && nearLng != null) {
                filtered.sort(Comparator.comparingDouble(m -> 
                        m.geo() != null ? m.geo().distanceToKm(nearLat, nearLng) : Double.MAX_VALUE));
            }

            return PagedResponse.of(filtered, page, size);
        }

        // Fallback: direct upstream portal call if in-memory sync is pending
        log.warn("Index not yet fully synced. Falling back to live portal search API.");
        RawMeterSearchResponse searchResp = portalClient.searchMeters(query, page);
        List<Meter> meters = searchResp.data().stream()
                .map(raw -> new Meter(
                        raw.meterId(),
                        raw.serialNo(),
                        raw.make(),
                        raw.phaseType(),
                        raw.installStatus(),
                        null,
                        null,
                        raw.dtCode(),
                        null,
                        null
                ))
                .collect(Collectors.toList());

        return new PagedResponse<>(
                meters,
                searchResp.page(),
                searchResp.pageSize(),
                searchResp.total(),
                (int) Math.ceil((double) searchResp.total() / searchResp.pageSize()),
                searchResp.page() == 1,
                searchResp.page() >= Math.ceil((double) searchResp.total() / searchResp.pageSize())
        );
    }

    public Meter getMeterById(String meterId) {
        if (meterId == null || meterId.isBlank()) {
            throw new IllegalArgumentException("Meter ID cannot be empty");
        }

        Optional<Meter> indexed = syncService.findMeterById(meterId);
        if (indexed.isPresent()) {
            return indexed.get();
        }

        // Upstream fallback
        RawMeterSearchResponse search = portalClient.searchMeters(meterId, 1);
        if (search == null || search.data() == null || search.data().isEmpty()) {
            throw new ResourceNotFoundException("Meter not found with ID: " + meterId);
        }

        var match = search.data().stream()
                .filter(m -> meterId.equalsIgnoreCase(m.meterId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with ID: " + meterId));

        RawGeoResponse geoResp = portalClient.getMeterGeo(meterId);
        GeoLocation geo = null;
        if (geoResp != null && geoResp.data() != null) {
            geo = new GeoLocation(geoResp.data().getLatitudeAsDouble(), geoResp.data().getLongitudeAsDouble());
        }

        return new Meter(
                match.meterId(),
                match.serialNo(),
                match.make(),
                match.phaseType(),
                match.installStatus(),
                null,
                null,
                match.dtCode(),
                geo,
                null
        );
    }

    @Cacheable(value = "meter-detail", key = "#meterId")
    public MeterDetail getMeterDetail(String meterId) {
        Meter meter = getMeterById(meterId);
        List<ConsumptionReading> readings = getConsumptionReadings(meterId);
        ConsumptionSummary summary = ConsumptionSummary.compute(readings);

        return new MeterDetail(meter, summary, readings.subList(0, Math.min(20, readings.size())));
    }

    @Cacheable(value = "meter-energy", key = "#meterId")
    public List<ConsumptionReading> getConsumptionReadings(String meterId) {
        try {
            RawEnergyResponse energyResp = portalClient.getMeterEnergy(meterId);
            if (energyResp == null || energyResp.data() == null) {
                return List.of();
            }

            return energyResp.data().stream()
                    .map(this::mapToReading)
                    .collect(Collectors.toList());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not retrieve energy readings for meter {}: {}", meterId, e.getMessage());
            return List.of();
        }
    }

    private ConsumptionReading mapToReading(RawEnergyRecord raw) {
        return ConsumptionReading.fromRaw(
                raw.timestamp(),
                raw.parseKwh(),
                raw.parseKvah(),
                raw.parseVoltR()
        );
    }

    private boolean matchesText(Meter m, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase().trim();
        return (m.meterId() != null && m.meterId().toLowerCase().contains(q)) ||
               (m.serialNo() != null && m.serialNo().toLowerCase().contains(q)) ||
               (m.dtCode() != null && m.dtCode().toLowerCase().contains(q));
    }

    private boolean matchesMake(Meter m, String make) {
        if (make == null || make.isBlank()) return true;
        return m.make() != null && m.make().equalsIgnoreCase(make.trim());
    }

    private boolean matchesPhase(Meter m, String phase) {
        if (phase == null || phase.isBlank()) return true;
        return m.phaseType() != null && m.phaseType().equalsIgnoreCase(phase.trim());
    }

    private boolean matchesStatus(Meter m, String status) {
        if (status == null || status.isBlank()) return true;
        return m.installStatus() != null && m.installStatus().equalsIgnoreCase(status.trim());
    }

    private boolean matchesDt(Meter m, String dtCode) {
        if (dtCode == null || dtCode.isBlank()) return true;
        return m.dtCode() != null && m.dtCode().equalsIgnoreCase(dtCode.trim());
    }

    private boolean matchesRadius(Meter m, Double nearLat, Double nearLng, Double radiusKm) {
        if (nearLat == null || nearLng == null || radiusKm == null) return true;
        if (m.geo() == null || !m.geo().isValid()) return false;
        return m.geo().distanceToKm(nearLat, nearLng) <= radiusKm;
    }
}
