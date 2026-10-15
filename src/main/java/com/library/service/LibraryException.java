package com.library.service;

/** A business-rule failure whose message is safe to show to the user. */
public class LibraryException extends RuntimeException {
    public LibraryException(String message) {
        super(message);
    }
}
