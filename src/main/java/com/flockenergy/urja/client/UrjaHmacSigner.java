package com.flockenergy.urja.client;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Component
public class UrjaHmacSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public record HmacSignature(String timestamp, String signature) {}

    public HmacSignature sign(String method, String path, String queryString, String secret) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        return sign(method, path, queryString, timestamp, secret);
    }

    public HmacSignature sign(String method, String path, String queryString, String timestamp, String secret) {
        try {
            String message = String.join("\n", method, path, queryString, timestamp);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return new HmacSignature(timestamp, hexString.toString());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }
}
