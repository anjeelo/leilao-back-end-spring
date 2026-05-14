// src/main/java/com/leiloai/exception/UnauthorizedException.java
package com.leiloai.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}