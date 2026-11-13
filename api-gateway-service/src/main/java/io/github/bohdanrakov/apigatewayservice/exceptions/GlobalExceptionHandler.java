package io.github.bohdanrakov.apigatewayservice.exceptions;

import io.github.bohdanrakov.apigatewayservice.dtos.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String NOT_FOUND_MESSAGE = "The requested resource was not found";
    private static final String UNAVAILABLE_MESSAGE = "The service is temporarily unavailable. Please try again later";
    private static final String TIMEOUT_MESSAGE = "The service took too long to respond. Please try again later";
    private static final String BAD_GATEWAY_MESSAGE = "The service returned an invalid response. Please try again later";
    private static final String INTERNAL_ERROR_MESSAGE = "Internal server error";

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> undefinedRoute(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> loadBalancerFailure(HttpServerErrorException ex) {
        logger.warn("Gateway could not route the request: {}", ex.getMessage());
        String message = ex.getStatusCode().isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)
                ? UNAVAILABLE_MESSAGE
                : BAD_GATEWAY_MESSAGE;
        return error(ex.getStatusCode(), message);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> backendAccessFailure(ResourceAccessException ex) {
        logger.warn("Gateway could not reach the backend: {}", ex.getMessage());
        if (hasCause(ex, ConnectException.class, UnknownHostException.class, NoRouteToHostException.class)) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE_MESSAGE);
        }
        if (hasCause(ex, SocketTimeoutException.class, HttpTimeoutException.class)) {
            return error(HttpStatus.GATEWAY_TIMEOUT, TIMEOUT_MESSAGE);
        }
        return error(HttpStatus.BAD_GATEWAY, BAD_GATEWAY_MESSAGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpectedFailure(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(status.value());
            return error(status, resolved != null ? resolved.getReasonPhrase() : "Request could not be processed");
        }
        logger.error("Internal server error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE);
    }

    @SafeVarargs
    private static boolean hasCause(Throwable ex, Class<? extends Throwable>... types) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            for (Class<? extends Throwable> type : types) {
                if (type.isInstance(current)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatusCode status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(message, Integer.toString(status.value())));
    }
}
