package io.github.bohdanrakov.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${aws.s3.url}")
    private String url;

    @Value("${aws.s3.region}")
    private String region;


    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(url))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .forcePathStyle(true)
                .build();
    }

}
