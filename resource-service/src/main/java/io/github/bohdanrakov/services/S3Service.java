package io.github.bohdanrakov.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.util.List;

@Service
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucket;

    private final S3Client s3Client;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostConstruct
    void initializeBucket() {
        try {
            s3Client.headBucket(b -> b.bucket(bucket));
        } catch (NoSuchBucketException ex) {
            s3Client.createBucket(b -> b.bucket(bucket));
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
