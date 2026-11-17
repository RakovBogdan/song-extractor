package io.github.bohdanrakov.exceptions;

import org.springframework.http.HttpStatus;

public class APIException extends RuntimeException {
    private final HttpStatus httpStatus;

    public APIException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
