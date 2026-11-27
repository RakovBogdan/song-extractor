package io.github.bohdanrakov.services;

import io.github.bohdanrakov.models.Storage;
import io.github.bohdanrakov.repositories.StorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

@Service
public class S3BucketService {

    private static final Logger logger = LoggerFactory.getLogger(S3BucketService.class);

    private final S3Client s3Client;
    private final StorageRepository storageRepository;

    public S3BucketService(S3Client s3Client, StorageRepository storageRepository) {
        this.s3Client = s3Client;
        this.storageRepository = storageRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    void initializeBuckets() {
        storageRepository.findAll().stream()
                .map(Storage::getBucket)
                .distinct()
                .forEach(this::createBucketIfMissing);
    }

    public void createBucketIfMissing(String bucket) {
        try {
            s3Client.createBucket(b -> b.bucket(bucket));
            logger.info("S3 bucket {} created", bucket);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ex) {
            logger.info("S3 bucket {} already exists", bucket);
        }
    }
}
