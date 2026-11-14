package io.github.bohdanrakov.services;

import io.github.bohdanrakov.dtos.StorageDTO;
import io.github.bohdanrakov.exceptions.StorageNotFoundException;
import io.github.bohdanrakov.models.StorageType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class StorageServiceCallerService {

    private final RestClient restClient;

    public StorageServiceCallerService(@Qualifier("storageRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class},
            maxRetries = 5,
            delay = 300,
            jitter = 10,
            multiplier = 2,
            maxDelay = 2000
    )
    public StorageDTO getStorage(StorageType storageType) {
        List<StorageDTO> storages = restClient.get()
                .uri("/storages")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (storages == null) {
            throw new StorageNotFoundException("Storage service returned no storages");
        }

        return storages.stream()
                .filter(storage -> storage.storageType() == storageType)
                .findFirst()
                .orElseThrow(() -> new StorageNotFoundException("No storage of type " + storageType + " is configured"));
    }
}
