package io.github.bohdanrakov.services;

import io.github.bohdanrakov.dtos.StorageDTO;
import io.github.bohdanrakov.exceptions.StorageNotFoundException;
import io.github.bohdanrakov.models.StorageType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class StorageServiceCallerService {

    private static final Logger logger = LoggerFactory.getLogger(StorageServiceCallerService.class);

    private final RestClient restClient;

    @Value("${storage.service.stub.staging.bucket}")
    private String stubStagingBucket;

    @Value("${storage.service.stub.staging.path}")
    private String stubStagingPath;

    @Value("${storage.service.stub.permanent.bucket}")
    private String stubPermanentBucket;

    @Value("${storage.service.stub.permanent.path}")
    private String stubPermanentPath;

    public StorageServiceCallerService(@Qualifier("storageRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "storageService", fallbackMethod = "getStubStorage")
    public StorageDTO getStorage(StorageType storageType) {
        List<StorageDTO> storages = restClient.get()
                .uri("/storages")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return findStorageByType(storages, storageType);
    }

    private StorageDTO getStubStorage(StorageType storageType, Throwable ex) {
        logger.warn("Storage service is unavailable, using stub {} storage: {}", storageType, ex.toString());
        return findStorageByType(stubStorages(), storageType);
    }

    private StorageDTO findStorageByType(List<StorageDTO> storages, StorageType storageType) {
        return storages.stream()
                .filter(storage -> storage.storageType() == storageType)
                .findFirst()
                .orElseThrow(() -> new StorageNotFoundException("No storage of type " + storageType + " is configured"));
    }

    private List<StorageDTO> stubStorages() {
        return List.of(
                new StorageDTO(1L, StorageType.STAGING, stubStagingBucket, stubStagingPath),
                new StorageDTO(2L, StorageType.PERMANENT, stubPermanentBucket, stubPermanentPath));
    }
}
