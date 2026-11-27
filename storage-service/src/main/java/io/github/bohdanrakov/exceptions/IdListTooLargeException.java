package io.github.bohdanrakov.exceptions;

import org.springframework.http.HttpStatus;

public class IdListTooLargeException extends APIException {
    public IdListTooLargeException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
