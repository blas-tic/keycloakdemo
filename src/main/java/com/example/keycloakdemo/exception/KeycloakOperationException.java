package com.example.keycloakdemo.exception;

public class KeycloakOperationException extends RuntimeException {
    public KeycloakOperationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public KeycloakOperationException(String message) {
        super(message);
    }
}
