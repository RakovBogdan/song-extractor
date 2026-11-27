package io.github.bohdanrakov.exceptions;

import org.springframework.http.HttpStatus;

public class StorageNotFoundException extends APIException {
    public StorageNotFoundException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
