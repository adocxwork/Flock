package com.flockenergy.urja;

import com.flockenergy.urja.client.UrjaHmacSigner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrjaHmacSignerTest {

    private final UrjaHmacSigner signer = new UrjaHmacSigner();

    @Test
    void testHmacSignatureGeneration() {
        String method = "GET";
        String path = "/portal/export";
        String qs = "page=1";
        String timestamp = "1730178668";
        String secret = "I3dZPPf5CgTp7JyGNMI8i6z8LFR7TmSR";

        UrjaHmacSigner.HmacSignature sig = signer.sign(method, path, qs, timestamp, secret);

        assertNotNull(sig);
        assertEquals(timestamp, sig.timestamp());
        assertNotNull(sig.signature());
        assertEquals(64, sig.signature().length(), "HMAC-SHA256 signature must be 64 hex characters");
    }

    @Test
    void testDeterministicSignature() {
        String method = "GET";
        String path = "/portal/export";
        String qs = "page=1";
        String timestamp = "1700000000";
        String secret = "test-secret-key-123";

        UrjaHmacSigner.HmacSignature sig1 = signer.sign(method, path, qs, timestamp, secret);
        UrjaHmacSigner.HmacSignature sig2 = signer.sign(method, path, qs, timestamp, secret);

        assertEquals(sig1.signature(), sig2.signature());
    }
}
