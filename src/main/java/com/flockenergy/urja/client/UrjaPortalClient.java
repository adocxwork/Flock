package com.flockenergy.urja.client;

import com.flockenergy.urja.dto.*;

public interface UrjaPortalClient {

    RawMeterSearchResponse searchMeters(String query, int page);

    RawGeoResponse getMeterGeo(String meterId);

    RawEnergyResponse getMeterEnergy(String meterId);

    RawDtsResponse getTransformers(int page);

    RawKeysResponse getSigningKey();

    RawExportResponse exportAllMeters();
}
