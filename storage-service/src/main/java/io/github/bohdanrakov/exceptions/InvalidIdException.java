package io.github.bohdanrakov.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidIdException extends APIException {
    public InvalidIdException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
