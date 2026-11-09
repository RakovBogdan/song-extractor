package io.github.bohdanrakov.apigatewayservice.exceptions;

import io.github.bohdanrakov.apigatewayservice.dtos.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class GlobalExceptionHandlerTest {

    private static final String UNAVAILABLE = "The service is temporarily unavailable. Please try again later";
    private static final String TIMEOUT = "The service took too long to respond. Please try again later";
    private static final String BAD_GATEWAY = "The service returned an invalid response. Please try again later";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void undefinedRouteReturnsNotFound() {
        var ex = new NoResourceFoundException(HttpMethod.GET, "/nope", "nope");

        assertError(handler.undefinedRoute(ex), HttpStatus.NOT_FOUND, "The requested resource was not found");
    }

    @Test
    void missingInstanceReturnsServiceUnavailableWithoutServiceName() {
        var ex = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to find instance for song-service");

        assertError(handler.loadBalancerFailure(ex), HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
    }

    @Test
    void otherLoadBalancerStatusIsKeptWithGenericMessage() {
        var ex = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "song-service:8082 failed");

        assertError(handler.loadBalancerFailure(ex), HttpStatus.INTERNAL_SERVER_ERROR, BAD_GATEWAY);
    }

    static Stream<Arguments> backendFailures() {
        return Stream.of(
                arguments(new ConnectException("song-service:8082 refused"), HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE),
                arguments(new UnknownHostException("song-service"), HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE),
                arguments(new NoRouteToHostException("song-service"), HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE),
                arguments(new IOException("wrapper", new ConnectException("refused")),
                        HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE),
                arguments(new SocketTimeoutException("song-service"), HttpStatus.GATEWAY_TIMEOUT, TIMEOUT),
                arguments(new HttpTimeoutException("song-service"), HttpStatus.GATEWAY_TIMEOUT, TIMEOUT),
                arguments(new IOException("song-service closed the stream"), HttpStatus.BAD_GATEWAY, BAD_GATEWAY));
    }

    @ParameterizedTest
    @MethodSource("backendFailures")
    void backendFailureIsMappedFromItsCause(IOException cause, HttpStatus status, String message) {
        var ex = new ResourceAccessException("I/O error on GET http://song-service:8082", cause);

        assertError(handler.backendAccessFailure(ex), status, message);
    }

    @Test
    void springWebErrorKeepsItsOwnStatus() {
        var ex = new HttpMediaTypeNotSupportedException("text/foo is not supported");

        assertError(handler.unexpectedFailure(ex), HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
    }

    @Test
    void unexpectedFailureHidesInternalDetails() {
        var ex = new IllegalStateException("password=secret at song-db:5432");

        assertError(handler.unexpectedFailure(ex), HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private static void assertError(ResponseEntity<ErrorResponse> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(message, Integer.toString(status.value())));
    }
}
