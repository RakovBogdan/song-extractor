package io.github.bohdanrakov.resourceprocessor.exceptions;

public class ResourceProcessedEventNotSentException extends RuntimeException {
    public ResourceProcessedEventNotSentException(String message) {
        super(message);
    }
}
