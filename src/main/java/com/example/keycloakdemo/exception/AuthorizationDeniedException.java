package com.example.keycloakdemo.exception;

public class AuthorizationDeniedException extends RuntimeException{
   public AuthorizationDeniedException(String message, Throwable cause) {
      super(message, cause);
   }

   public AuthorizationDeniedException(String message) {
        super(message);
    }
}
