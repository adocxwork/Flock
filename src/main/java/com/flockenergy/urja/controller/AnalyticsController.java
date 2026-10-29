package com.flockenergy.urja.controller;

import com.flockenergy.urja.model.AnalyticsOverview;
import com.flockenergy.urja.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Operational KPIs, grid health score, equipment breakdown")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @Operation(
            summary = "Get grid operational overview",
            description = "Provides high-level operational KPIs across all smart meters and transformers, including health score %, equipment make breakdown, phase breakdown, and build distribution."
    )
    public ResponseEntity<AnalyticsOverview> getOverview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }
}
