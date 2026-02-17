package com.example.keycloakdemo.exception;

public class HttpClientErrorException extends RuntimeException {
   public HttpClientErrorException(String message, Throwable cause) {
      super(message, cause);
   }

   public HttpClientErrorException(String message) {
        super(message);
    }

}
