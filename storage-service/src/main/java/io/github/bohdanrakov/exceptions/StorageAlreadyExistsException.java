package io.github.bohdanrakov.exceptions;

import org.springframework.http.HttpStatus;

public class StorageAlreadyExistsException extends APIException {
    public StorageAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
