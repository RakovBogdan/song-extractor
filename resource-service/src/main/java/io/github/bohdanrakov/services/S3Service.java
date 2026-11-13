package io.github.bohdanrakov.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.util.List;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Value("${aws.s3.bucket-name}")
    private String bucket;

    private final S3Client s3Client;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostConstruct
    void initializeBucket() {
        try {
            s3Client.createBucket(b -> b.bucket(bucket));
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ex) {
            logger.info("S3 bucket {} already exists", bucket);
        }
    }

    public void storeMP3File(String s3Key, byte[] mp3content) {
        s3Client.putObject(b -> b
                .bucket(bucket)
                .key(s3Key)
                .contentType("audio/mpeg"), RequestBody.fromBytes(mp3content));
    }

    public byte[] getMP3FileContent(String s3Key) {
        return s3Client.getObjectAsBytes(b -> b
                .bucket(bucket)
                .key(s3Key))
                .asByteArray();
    }

    public void deleteMp3Files(List<String> s3Keys) {
        List<ObjectIdentifier> s3Identifiers = s3Keys.stream()
              .map(k -> ObjectIdentifier.builder().key(k).build())
              .toList();
        s3Client.deleteObjects(b -> b
                .bucket(bucket)
                .delete(d -> d.objects(s3Identifiers)));
    }
}
