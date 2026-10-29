package com.flockenergy.urja;

import com.flockenergy.urja.model.ConsumptionReading;
import com.flockenergy.urja.model.ConsumptionSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsumptionSummaryTest {

    @Test
    void testComputeConsumptionSummary() {
        List<ConsumptionReading> readings = List.of(
                ConsumptionReading.fromRaw("24/06/2026 00:00", 100.0, 110.0, 230.0),
                ConsumptionReading.fromRaw("24/06/2026 00:30", 102.5, 112.8, 228.0),
                ConsumptionReading.fromRaw("24/06/2026 01:00", 105.0, 115.5, 232.0)
        );

        ConsumptionSummary summary = ConsumptionSummary.compute(readings);

        assertEquals(3, summary.readingCount());
        assertEquals("24/06/2026 00:00", summary.firstTimestamp());
        assertEquals("24/06/2026 01:00", summary.latestTimestamp());
        assertEquals(5.0, summary.totalEnergyConsumedKwh());
        assertEquals(230.0, summary.avgVoltage());
        assertEquals(228.0, summary.minVoltage());
        assertEquals(232.0, summary.maxVoltage());
        assertEquals("NORMAL", summary.voltageStatus());
    }

    @Test
    void testUnderVoltageDetection() {
        List<ConsumptionReading> readings = List.of(
                ConsumptionReading.fromRaw("24/06/2026 00:00", 100.0, 110.0, 195.0),
                ConsumptionReading.fromRaw("24/06/2026 00:30", 102.0, 112.0, 220.0)
        );

        ConsumptionSummary summary = ConsumptionSummary.compute(readings);

        assertEquals("UNDER_VOLTAGE_RISK", summary.voltageStatus());
        assertFalse(summary.anomalyFlags().isEmpty());
    }

    @Test
    void testEmptyReadings() {
        ConsumptionSummary summary = ConsumptionSummary.compute(List.of());
        assertEquals(0, summary.readingCount());
        assertEquals("NO_DATA", summary.voltageStatus());
    }
}
