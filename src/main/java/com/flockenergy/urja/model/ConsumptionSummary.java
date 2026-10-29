package com.flockenergy.urja.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConsumptionSummary(
        int readingCount,
        String firstTimestamp,
        String latestTimestamp,
        Double firstKwh,
        Double latestKwh,
        Double totalEnergyConsumedKwh,
        Double latestKvah,
        Double latestVoltR,
        Double minVoltage,
        Double maxVoltage,
        Double avgVoltage,
        String voltageStatus,
        List<String> anomalyFlags
) {
    public static ConsumptionSummary compute(List<ConsumptionReading> readings) {
        if (readings == null || readings.isEmpty()) {
            return new ConsumptionSummary(
                    0, null, null, null, null, null, null, null, null, null, null, "NO_DATA", List.of()
            );
        }

        ConsumptionReading first = readings.get(0);
        ConsumptionReading latest = readings.get(readings.size() - 1);

        Double firstKwh = first.kwh();
        Double latestKwh = latest.kwh();
        Double totalConsumed = (firstKwh != null && latestKwh != null) ? Math.round((latestKwh - firstKwh) * 100.0) / 100.0 : null;

        double sumVolt = 0.0;
        int voltCount = 0;
        double minVolt = Double.MAX_VALUE;
        double maxVolt = Double.MIN_VALUE;

        java.util.List<String> anomalies = new java.util.ArrayList<>();

        for (ConsumptionReading r : readings) {
            if (r.voltR() != null) {
                double v = r.voltR();
                sumVolt += v;
                voltCount++;
                if (v < minVolt) minVolt = v;
                if (v > maxVolt) maxVolt = v;

                if (v < 205.0 && anomalies.size() < 3) {
                    anomalies.add("Under-voltage observed: " + v + "V at " + r.timestamp());
                } else if (v > 255.0 && anomalies.size() < 3) {
                    anomalies.add("Over-voltage observed: " + v + "V at " + r.timestamp());
                }
            }
        }

        Double avgV = voltCount > 0 ? Math.round((sumVolt / voltCount) * 10.0) / 10.0 : null;
        Double minV = voltCount > 0 ? minVolt : null;
        Double maxV = voltCount > 0 ? maxVolt : null;

        String vStatus = "NORMAL";
        if (minV != null && minV < 205.0) {
            vStatus = "UNDER_VOLTAGE_RISK";
        } else if (maxV != null && maxV > 255.0) {
            vStatus = "OVER_VOLTAGE_RISK";
        }

        return new ConsumptionSummary(
                readings.size(),
                first.timestamp(),
                latest.timestamp(),
                firstKwh,
                latestKwh,
                totalConsumed,
                latest.kvah(),
                latest.voltR(),
                minV,
                maxV,
                avgV,
                vStatus,
                anomalies
        );
    }
}
