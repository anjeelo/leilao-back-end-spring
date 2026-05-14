// src/main/java/com/leiloai/exception/AuctionNotFoundException.java
package com.leiloai.exception;

public class AuctionNotFoundException extends RuntimeException {

    public AuctionNotFoundException(String message) {
        super(message);
    }

    public AuctionNotFoundException(Long id) {
        super("Leilão não encontrado com ID: " + id);
    }
}