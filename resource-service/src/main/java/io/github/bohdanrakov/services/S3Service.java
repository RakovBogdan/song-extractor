package io.github.bohdanrakov.services;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.util.List;

@Service
public class S3Service {

    private final S3Client s3Client;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void storeMP3File(String bucket, String s3Key, byte[] mp3content) {
        s3Client.putObject(b -> b
                .bucket(bucket)
                .key(s3Key)
                .contentType("audio/mpeg"), RequestBody.fromBytes(mp3content));
    }

    public byte[] getMP3FileContent(String bucket, String s3Key) {
        return s3Client.getObjectAsBytes(b -> b
                .bucket(bucket)
                .key(s3Key))
                .asByteArray();
    }

    public void copyMP3File(String sourceBucket, String sourceKey, String destinationBucket, String destinationKey) {
        s3Client.copyObject(b -> b
                .sourceBucket(sourceBucket)
                .sourceKey(sourceKey)
                .destinationBucket(destinationBucket)
                .destinationKey(destinationKey));
    }

    public void deleteMp3Files(String bucket, List<String> s3Keys) {
        if (s3Keys.isEmpty()) {
            return;
        }
        List<ObjectIdentifier> s3Identifiers = s3Keys.stream()
              .map(k -> ObjectIdentifier.builder().key(k).build())
              .toList();
        s3Client.deleteObjects(b -> b
                .bucket(bucket)
                .delete(d -> d.objects(s3Identifiers)));
    }
}
