package com.flockenergy.urja;

import com.flockenergy.urja.client.UrjaPortalClient;
import com.flockenergy.urja.model.GeoLocation;
import com.flockenergy.urja.model.Meter;
import com.flockenergy.urja.model.PagedResponse;
import com.flockenergy.urja.service.MeterDataSyncService;
import com.flockenergy.urja.service.MeterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class MeterServiceTest {

    private MeterDataSyncService syncService;
    private UrjaPortalClient portalClient;
    private MeterService meterService;

    @BeforeEach
    void setUp() {
        syncService = Mockito.mock(MeterDataSyncService.class);
        portalClient = Mockito.mock(UrjaPortalClient.class);
        meterService = new MeterService(syncService, portalClient);

        Meter m1 = new Meter("J100000", "SE33962", "HPL", "single", "Installed", "Whole Current", "legacy", "DT-001", new GeoLocation(26.9389, 75.8309), null);
        Meter m2 = new Meter("J100001", "GE84132", "L&T", "three", "Faulty", "CT Operated", "legacy", "DT-002", new GeoLocation(26.8221, 75.9071), null);
        Meter m3 = new Meter("J100002", "AL28136", "Genus", "single", "Decommissioned", "Whole Current", "v2", "DT-001", new GeoLocation(26.8402, 75.7146), null);

        when(syncService.isSynced()).thenReturn(true);
        when(syncService.getAllMeters()).thenReturn(List.of(m1, m2, m3));
    }

    @Test
    void testFilterByMake() {
        PagedResponse<Meter> res = meterService.searchAndFilterMeters(null, "HPL", null, null, null, null, null, null, 1, 10);
        assertEquals(1, res.totalElements());
        assertEquals("J100000", res.content().get(0).meterId());
    }

    @Test
    void testFilterByStatus() {
        PagedResponse<Meter> res = meterService.searchAndFilterMeters(null, null, null, "Faulty", null, null, null, null, 1, 10);
        assertEquals(1, res.totalElements());
        assertEquals("J100001", res.content().get(0).meterId());
    }

    @Test
    void testFilterByDtCode() {
        PagedResponse<Meter> res = meterService.searchAndFilterMeters(null, null, null, null, "DT-001", null, null, null, 1, 10);
        assertEquals(2, res.totalElements());
    }

    @Test
    void testGeospatialRadiusQuery() {
        // Query near DT-001 with 5km radius: only J100000 is within 5km
        PagedResponse<Meter> res = meterService.searchAndFilterMeters(null, null, null, null, null, 26.9389, 75.8309, 5.0, 1, 10);
        assertEquals(1, res.totalElements());
        assertEquals("J100000", res.content().get(0).meterId());
    }
}
