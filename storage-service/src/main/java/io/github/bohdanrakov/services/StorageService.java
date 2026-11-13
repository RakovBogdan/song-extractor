package io.github.bohdanrakov.services;

import io.github.bohdanrakov.dtos.StorageCreateRequest;
import io.github.bohdanrakov.dtos.StorageResponse;
import io.github.bohdanrakov.exceptions.IdListTooLargeException;
import io.github.bohdanrakov.exceptions.InvalidIdException;
import io.github.bohdanrakov.exceptions.StorageAlreadyExistsException;
import io.github.bohdanrakov.mappers.StorageMapper;
import io.github.bohdanrakov.models.Storage;
import io.github.bohdanrakov.repositories.StorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);
    private final StorageRepository storageRepository;
    private final StorageMapper storageMapper;
    private final S3BucketService s3BucketService;

    public StorageService(StorageRepository storageRepository, StorageMapper storageMapper,
                          S3BucketService s3BucketService) {
        this.storageRepository = storageRepository;
        this.storageMapper = storageMapper;
        this.s3BucketService = s3BucketService;
    }

    @Transactional
    public Long createStorage(StorageCreateRequest storageCreateRequest) {
        logger.info("Creating storage");
        if (storageRepository.existsByBucketAndPath(storageCreateRequest.bucket(), storageCreateRequest.path())) {
            throw new StorageAlreadyExistsException("Storage with bucket '" + storageCreateRequest.bucket()
                    + "' and path '" + storageCreateRequest.path() + "' already exists");
        }
        s3BucketService.createBucketIfMissing(storageCreateRequest.bucket());
        Storage savedStorage = storageRepository.save(storageMapper.toEntity(storageCreateRequest));

        return savedStorage.getId();
    }

    public List<StorageResponse> getAllStorages() {
        return storageMapper.toDtos(storageRepository.findAll());
    }

    @Transactional
    public List<Long> deleteStoragesByIds(String ids) {
        logger.info("Deleting storages");
        if (ids.length() >= 200) {
            throw new IdListTooLargeException("CSV string is too long: received " + ids.length()
                    + " characters, maximum allowed is 199");
        }

        List<Long> parsedIds = new ArrayList<>();

        for (String id : ids.split(",")) {
            if (!isValidLong(id)) {
                throw new InvalidIdException("Invalid ID format: '" + id + "'. Only positive integers are allowed");
            }
            parsedIds.add(Long.parseLong(id));
        }

        List<Long> idsToDelete = storageRepository.findExistingIds(parsedIds);
        storageRepository.deleteAllById(idsToDelete);
        return idsToDelete;
    }

    private boolean isValidLong(String number) {
        if (number.isEmpty() || number.length() > 18) {
            return false;
        }
        for (int i = 0; i < number.length(); i++) {
            if (!Character.isDigit(number.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
