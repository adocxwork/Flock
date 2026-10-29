package com.flockenergy.urja.client;

import com.flockenergy.urja.config.UrjaPortalProperties;
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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UrjaSessionManager {

    private static final Logger log = LoggerFactory.getLogger(UrjaSessionManager.class);
    private static final Pattern SESSION_COOKIE_PATTERN = Pattern.compile("(__Secure-better-auth\\.session_token=[^;]+)");

    private final UrjaPortalProperties properties;
    private final HttpClient httpClient;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String sessionCookie;
    private volatile Instant expiresAt = Instant.MIN;

    public UrjaSessionManager(UrjaPortalProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Retrieves a valid session cookie for requests to the upstream Urja portal.
     * Authenticates if session is absent, expired, or near expiry (within 5 minutes).
     */
    public String getSessionCookie() {
        if (sessionCookie != null && Instant.now().isBefore(expiresAt.minus(Duration.ofMinutes(5)))) {
            return sessionCookie;
        }

        lock.lock();
        try {
            if (sessionCookie != null && Instant.now().isBefore(expiresAt.minus(Duration.ofMinutes(5)))) {
                return sessionCookie;
            }
            authenticate();
            return sessionCookie;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Invalidate current session token to force re-authentication on the next request.
     */
    public void invalidateSession() {
        lock.lock();
        try {
            log.info("Invalidating Urja Portal session token");
            this.sessionCookie = null;
            this.expiresAt = Instant.MIN;
        } finally {
            lock.unlock();
        }
    }

    private void authenticate() {
        String loginUrl = properties.getBaseUrl() + "/login";
        log.info("Authenticating with Urja Meter Ops portal at: {}", loginUrl);

        try {
            String formBody = "email=" + URLEncoder.encode(properties.getUsername(), StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(properties.getPassword(), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(loginUrl))
                    .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Origin", properties.getBaseUrl())
                    .header("Referer", loginUrl)
                    .header("x-sveltekit-action", "true")
                    .header("Accept", "application/json")
                    .header("User-Agent", "FlockEnergy-UrjaAPI/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 303) {
                log.error("Authentication failed. HTTP status: {}, Body: {}", response.statusCode(), response.body());
                throw new UpstreamPortalException("Failed to authenticate with Urja portal: HTTP " + response.statusCode());
            }

            List<String> setCookies = response.headers().allValues("set-cookie");
            String extractedToken = null;
            for (String cookie : setCookies) {
                Matcher matcher = SESSION_COOKIE_PATTERN.matcher(cookie);
                if (matcher.find()) {
                    extractedToken = matcher.group(1);
                    break;
                }
            }

            if (extractedToken == null) {
                log.error("Authentication succeeded status-wise but __Secure-better-auth.session_token was not present in Set-Cookie headers: {}", setCookies);
                throw new UpstreamPortalException("Urja portal login succeeded but no session token cookie was returned.");
            }

            this.sessionCookie = extractedToken;
            // Better-Auth cookie default is 3600s (1 hour)
            this.expiresAt = Instant.now().plus(Duration.ofMinutes(55));
            log.info("Successfully authenticated with Urja portal. Session token acquired.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamPortalException("Authentication interrupted", e);
        } catch (Exception e) {
            if (e instanceof UpstreamPortalException upe) {
                throw upe;
            }
            throw new UpstreamPortalException("Error communicating with Urja portal during login: " + e.getMessage(), e);
        }
    }
}
