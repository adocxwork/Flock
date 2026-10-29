package com.flockenergy.urja.exception;

public class UpstreamPortalException extends RuntimeException {
    public UpstreamPortalException(String message) {
        super(message);
    }

    public UpstreamPortalException(String message, Throwable cause) {
        super(message, cause);
    }
}
