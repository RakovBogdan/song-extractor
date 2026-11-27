package io.github.bohdanrakov.dtos;

import java.util.Map;

public record ValidationErrorResponse(String errorMessage, Map<String, String> details, String errorCode) {
}
