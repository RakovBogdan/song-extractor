package io.github.bohdanrakov.resourceprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ResourceServiceCallerService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceCallerService.class);
    private final RestClient restClient;

    public ResourceServiceCallerService(@Qualifier("resourceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class},
            maxRetries = 5,
            delay = 300,
            jitter = 10,
            multiplier = 2,
            maxDelay = 2000
    )
    public byte[] getMP3ByteContent(Long id) {
        return restClient.get()
                .uri("/resources/{id}", id)
                .retrieve()
                .body(byte[].class);
    }
}
