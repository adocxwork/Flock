package com.flockenergy.urja.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flockenergy.urja.config.UrjaPortalProperties;
import com.flockenergy.urja.dto.*;
import com.flockenergy.urja.exception.ResourceNotFoundException;
import com.flockenergy.urja.exception.UpstreamPortalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Component
public class UrjaPortalClientImpl implements UrjaPortalClient {

    private static final Logger log = LoggerFactory.getLogger(UrjaPortalClientImpl.class);

    private final UrjaPortalProperties properties;
    private final UrjaSessionManager sessionManager;
    private final UrjaHmacSigner hmacSigner;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public UrjaPortalClientImpl(UrjaPortalProperties properties,
                               UrjaSessionManager sessionManager,
                               UrjaHmacSigner hmacSigner,
                               ObjectMapper objectMapper) {
        this.properties = properties;
        this.sessionManager = sessionManager;
        this.hmacSigner = hmacSigner;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
    }

    @Override
    public RawMeterSearchResponse searchMeters(String query, int page) {
        String encodedQuery = URLEncoder.encode(query != null ? query : "", StandardCharsets.UTF_8);
        String path = "/portal/meters/search?q=" + encodedQuery + "&page=" + Math.max(1, page);
        return executeGet(path, Map.of(), RawMeterSearchResponse.class);
    }

    @Override
    public RawGeoResponse getMeterGeo(String meterId) {
        String path = "/portal/meters/" + URLEncoder.encode(meterId, StandardCharsets.UTF_8) + "/geo";
        return executeGet(path, Map.of(), RawGeoResponse.class);
    }

    @Override
    public RawEnergyResponse getMeterEnergy(String meterId) {
        String path = "/portal/meters/" + URLEncoder.encode(meterId, StandardCharsets.UTF_8) + "/energy";
        return executeGet(path, Map.of(), RawEnergyResponse.class);
    }

    @Override
    public RawDtsResponse getTransformers(int page) {
        String path = "/portal/dts?page=" + Math.max(1, page);
        return executeGet(path, Map.of(), RawDtsResponse.class);
    }

    @Override
    public RawKeysResponse getSigningKey() {
        return executeGet("/portal/keys", Map.of(), RawKeysResponse.class);
    }

    @Override
    public RawExportResponse exportAllMeters() {
        RawKeysResponse keys = getSigningKey();
        if (keys == null || keys.data() == null || keys.data().signingSecret() == null) {
            throw new UpstreamPortalException("Failed to retrieve signing secret from Urja portal /portal/keys");
        }

        String secret = keys.data().signingSecret();
        String path = "/portal/export";
        String qs = "page=1";
        UrjaHmacSigner.HmacSignature hmac = hmacSigner.sign("GET", path, qs, secret);

        Map<String, String> headers = Map.of(
                "x-timestamp", hmac.timestamp(),
                "x-signature", hmac.signature()
        );

        return executeGet(path + "?" + qs, headers, RawExportResponse.class);
    }

    private <T> T executeGet(String path, Map<String, String> extraHeaders, Class<T> responseType) {
        return executeWithRetry(path, extraHeaders, responseType, true);
    }

    private <T> T executeWithRetry(String path, Map<String, String> extraHeaders, Class<T> responseType, boolean allowRetry) {
        String url = properties.getBaseUrl() + path;
        String cookie = sessionManager.getSessionCookie();

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                    .header("Cookie", cookie)
                    .header("Accept", "application/json")
                    .header("User-Agent", "FlockEnergy-UrjaAPI/1.0");

            extraHeaders.forEach(builder::header);

            HttpResponse<String> response = httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401 && allowRetry) {
                log.warn("Received 401 from {}. Session may have expired. Refreshing token and retrying...", path);
                sessionManager.invalidateSession();
                return executeWithRetry(path, extraHeaders, responseType, false);
            }

            if (response.statusCode() == 404) {
                throw new ResourceNotFoundException("Resource not found upstream at " + path);
            }

            if (response.statusCode() >= 400) {
                log.error("Urja portal error on GET {}: HTTP {}, Body: {}", path, response.statusCode(), response.body());
                throw new UpstreamPortalException("Urja portal error (" + response.statusCode() + "): " + response.body());
            }

            return objectMapper.readValue(response.body(), responseType);

        } catch (ResourceNotFoundException | UpstreamPortalException ex) {
            throw ex;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamPortalException("Request interrupted for " + path, e);
        } catch (Exception e) {
            throw new UpstreamPortalException("Failed to call Urja portal endpoint " + path + ": " + e.getMessage(), e);
        }
    }
}
