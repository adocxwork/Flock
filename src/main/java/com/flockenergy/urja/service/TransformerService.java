package com.flockenergy.urja.service;

import com.flockenergy.urja.dto.RawDtRecord;
import com.flockenergy.urja.dto.RawDtsResponse;
import com.flockenergy.urja.client.UrjaPortalClient;
import com.flockenergy.urja.exception.ResourceNotFoundException;
import com.flockenergy.urja.model.Meter;
import com.flockenergy.urja.model.Transformer;
import com.flockenergy.urja.model.TransformerDetail;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransformerService {

    private final MeterDataSyncService syncService;
    private final UrjaPortalClient portalClient;

    public TransformerService(MeterDataSyncService syncService, UrjaPortalClient portalClient) {
        this.syncService = syncService;
        this.portalClient = portalClient;
    }

    @Cacheable(value = "transformers")
    public List<Transformer> getAllTransformers() {
        if (syncService.isSynced()) {
            return syncService.getAllTransformers();
        }

        List<Transformer> result = new ArrayList<>();
        RawDtsResponse p1 = portalClient.getTransformers(1);
        if (p1 != null && p1.data() != null) {
            p1.data().forEach(raw -> result.add(toTransformer(raw)));
        }
        RawDtsResponse p2 = portalClient.getTransformers(2);
        if (p2 != null && p2.data() != null) {
            p2.data().forEach(raw -> result.add(toTransformer(raw)));
        }
        return result;
    }

    public TransformerDetail getTransformerDetail(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Transformer code cannot be empty");
        }

        Transformer transformer = syncService.findTransformerByCode(code.trim().toUpperCase())
                .orElseGet(() -> getAllTransformers().stream()
                        .filter(t -> t.code().equalsIgnoreCase(code.trim()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Transformer not found with code: " + code)));

        List<Meter> connectedMeters = syncService.findMetersByDtCode(transformer.code());

        return new TransformerDetail(transformer, connectedMeters);
    }

    private Transformer toTransformer(RawDtRecord raw) {
        return new Transformer(
                raw.code(),
                raw.name(),
                raw.feederCode(),
                raw.capacityKva(),
                0,
                0,
                0,
                0,
                "UNKNOWN"
        );
    }
}
