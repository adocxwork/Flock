package com.flockenergy.urja.service;

import com.flockenergy.urja.model.AnalyticsOverview;
import com.flockenergy.urja.model.Meter;
import com.flockenergy.urja.model.Transformer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final MeterDataSyncService syncService;

    public AnalyticsService(MeterDataSyncService syncService) {
        this.syncService = syncService;
    }

    public AnalyticsOverview getOverview() {
        List<Meter> meters = syncService.getAllMeters();
        List<Transformer> transformers = syncService.getAllTransformers();

        int total = meters.size();
        int installed = 0;
        int faulty = 0;
        int decommissioned = 0;

        for (Meter m : meters) {
            String s = m.installStatus();
            if ("Installed".equalsIgnoreCase(s)) installed++;
            else if ("Faulty".equalsIgnoreCase(s)) faulty++;
            else if ("Decommissioned".equalsIgnoreCase(s)) decommissioned++;
        }

        double healthScore = (installed + faulty > 0)
                ? Math.round(((double) installed / (installed + faulty) * 100.0) * 10.0) / 10.0
                : 100.0;

        int totalCapacity = transformers.stream()
                .mapToInt(Transformer::capacityKva)
                .sum();

        Map<String, Long> makeDist = meters.stream()
                .filter(m -> m.make() != null)
                .collect(Collectors.groupingBy(Meter::make, Collectors.counting()));

        Map<String, Long> phaseDist = meters.stream()
                .filter(m -> m.phaseType() != null)
                .collect(Collectors.groupingBy(Meter::phaseType, Collectors.counting()));

        Map<String, Long> statusDist = meters.stream()
                .filter(m -> m.installStatus() != null)
                .collect(Collectors.groupingBy(Meter::installStatus, Collectors.counting()));

        Map<String, Long> buildDist = meters.stream()
                .filter(m -> m.build() != null)
                .collect(Collectors.groupingBy(Meter::build, Collectors.counting()));

        return new AnalyticsOverview(
                total,
                installed,
                faulty,
                decommissioned,
                healthScore,
                transformers.size(),
                totalCapacity,
                makeDist,
                phaseDist,
                statusDist,
                buildDist
        );
    }
}
