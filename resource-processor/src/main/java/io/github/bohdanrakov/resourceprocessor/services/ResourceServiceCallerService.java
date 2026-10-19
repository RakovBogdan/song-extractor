package io.github.bohdanrakov.resourceprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ResourceServiceCallerService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceCallerService.class);
    private final RestClient restClient;

    public ResourceServiceCallerService(@Qualifier("resourceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public byte[] getMP3ByteContent(Long id) {
        try {
            return restClient.get()
                    .uri("/resources/{id}", id)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException ex) {
            logger.error("Failed to call resource-service to get mp3 byte content", ex);
            throw ex;
        }
    }
}
