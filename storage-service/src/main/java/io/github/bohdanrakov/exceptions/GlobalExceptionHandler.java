package io.github.bohdanrakov.exceptions;

import io.github.bohdanrakov.dtos.ErrorResponse;
import io.github.bohdanrakov.dtos.ValidationErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleApiException(MethodArgumentNotValidException ex) {
        Map<String, String> errorDetails = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errorDetails.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return new ResponseEntity<>(
                new ValidationErrorResponse("Validation error", errorDetails,
                        Integer.toString(HttpStatus.BAD_REQUEST.value())),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadableBody(HttpMessageNotReadableException ex) {
        return new ResponseEntity<>(
                new ErrorResponse("Malformed request body. storageType must be one of: STAGING, PERMANENT",
                        Integer.toString(HttpStatus.BAD_REQUEST.value())),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> missingParameter(MissingServletRequestParameterException ex) {
        return new ResponseEntity<>(
                new ErrorResponse("Required request parameter '" + ex.getParameterName() + "' is missing",
                        Integer.toString(HttpStatus.BAD_REQUEST.value())),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<ErrorResponse> handleApiException(APIException ex) {
        return new ResponseEntity<>(
                new ErrorResponse(ex.getMessage(), Integer.toString(ex.getHttpStatus().value())),
                ex.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> customInternalServerError(Exception ex) {
        logger.error("Internal server error", ex);
        return new ResponseEntity<>(
                new ErrorResponse("Internal server error",
                        Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value())),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
