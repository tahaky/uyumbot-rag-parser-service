package com.uyumbot.chunkingservice.exception;

public class ChunkingException extends RuntimeException {

    public ChunkingException(String message) {
        super(message);
    }

    public ChunkingException(String message, Throwable cause) {
        super(message, cause);
    }
}
