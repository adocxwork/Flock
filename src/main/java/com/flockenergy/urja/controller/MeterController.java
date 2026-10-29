package com.flockenergy.urja.controller;

import com.flockenergy.urja.model.*;
import com.flockenergy.urja.service.MeterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meters")
@Tag(name = "Meters", description = "Smart meter inventory, multi-criteria filtering, geospatial proximity queries, and consumption tracking")
public class MeterController {

    private final MeterService meterService;

    public MeterController(MeterService meterService) {
        this.meterService = meterService;
    }

    @GetMapping
    @Operation(
            summary = "Query and filter meters",
            description = """
                    Search and filter the smart meter fleet with pagination.
                    Supports text search, attribute filtering (make, phase, status, DT code),
                    and geospatial proximity radius search (e.g. find meters within X km of a coordinate).
                    """
    )
    public ResponseEntity<PagedResponse<Meter>> getMeters(
            @Parameter(description = "Text search query matching meter ID, serial number, or DT code")
            @RequestParam(required = false) String q,

            @Parameter(description = "Filter by manufacturer make (e.g. HPL, L&T, Genus, Allied, Secure)")
            @RequestParam(required = false) String make,

            @Parameter(description = "Filter by electrical phase (e.g. single, three)")
            @RequestParam(required = false) String phase,

            @Parameter(description = "Filter by installation status (Installed, Faulty, Decommissioned)")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filter by distribution transformer code (e.g. DT-001)")
            @RequestParam(required = false) String dtCode,

            @Parameter(description = "Geospatial latitude for proximity search")
            @RequestParam(required = false) Double nearLat,

            @Parameter(description = "Geospatial longitude for proximity search")
            @RequestParam(required = false) Double nearLng,

            @Parameter(description = "Radius in kilometers for proximity search (requires nearLat & nearLng)")
            @RequestParam(required = false) Double radiusKm,

            @Parameter(description = "Page number (1-based, default 1)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Page size (default 20, max 100)")
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResponse<Meter> response = meterService.searchAndFilterMeters(
                q, make, phase, status, dtCode, nearLat, nearLng, radiusKm, page, Math.min(100, Math.max(1, size))
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get meter details",
            description = "Retrieves canonical details for a specific smart meter, including nameplate parameters, geographical coordinates, network hierarchy breadcrumb, and consumption status."
    )
    public ResponseEntity<MeterDetail> getMeterDetail(
            @Parameter(description = "Meter ID (e.g. J100000)", required = true)
            @PathVariable String id
    ) {
        return ResponseEntity.ok(meterService.getMeterDetail(id));
    }

    @GetMapping("/{id}/consumption")
    @Operation(
            summary = "Get meter consumption history",
            description = "Fetches normalized 30-minute interval smart-meter readings (active kWh, apparent kVAh, and voltage) along with computed consumption summary analytics."
    )
    public ResponseEntity<ConsumptionHistoryResponse> getMeterConsumption(
            @Parameter(description = "Meter ID (e.g. J100000)", required = true)
            @PathVariable String id
    ) {
        Meter meter = meterService.getMeterById(id);
        List<ConsumptionReading> readings = meterService.getConsumptionReadings(id);
        ConsumptionSummary summary = ConsumptionSummary.compute(readings);
        return ResponseEntity.ok(new ConsumptionHistoryResponse(meter.meterId(), meter.serialNo(), summary, readings));
    }

    public record ConsumptionHistoryResponse(
            String meterId,
            String serialNo,
            ConsumptionSummary summary,
            List<ConsumptionReading> readings
    ) {}
}
