package io.github.bohdanrakov.services;

import io.github.bohdanrakov.events.ResourceStoredEventPublisher;
import io.github.bohdanrakov.exceptions.MP3FileUnparsableException;
import io.github.bohdanrakov.models.MP3File;
import io.github.bohdanrakov.repositories.MP3FileRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "eureka.client.enabled=false")
@Testcontainers
class MP3StorageServiceIntegrationTest {

    private static final String BUCKET_NAME = "mp3-storage";
    private static final String QUEUE_NAME = "exchange-resource-id.resource-processor";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:17-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath("../init-scripts/resource-db/init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Container
    static LocalStackContainer localstackContainer =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:4")).withServices("s3");

    private static S3Client s3TestClient;
    private static byte[] mp3Content;

    @Autowired
    private MP3StorageService mp3StorageService;

    @Autowired
    private MP3FileRepository mp3FileRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @MockitoSpyBean
    private ResourceStoredEventPublisher resourceStoredEventPublisher;

    @MockitoBean
    private SongServiceCallerService songServiceCallerService;

    @DynamicPropertySource
    static void s3Properties(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.url", () -> localstackContainer.getEndpoint().toString());
        registry.add("aws.s3.region", localstackContainer::getRegion);
        registry.add("aws.accessKeyId", localstackContainer::getAccessKey);
        registry.add("aws.secretAccessKey", localstackContainer::getSecretKey);
        registry.add("aws.s3.bucket-name", () -> BUCKET_NAME);
    }

    @BeforeAll
    static void before() throws IOException {
        s3TestClient = S3Client.builder()
                .endpointOverride(localstackContainer.getEndpoint())
                .region(Region.of(localstackContainer.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstackContainer.getAccessKey(), localstackContainer.getSecretKey())))
                .forcePathStyle(true)
                .build();

        try (InputStream in = MP3StorageServiceIntegrationTest.class.getResourceAsStream("/sample.mp3")) {
            mp3Content = Objects.requireNonNull(in).readAllBytes();
        }
    }

    @AfterAll
    static void after() {
        s3TestClient.close();
    }

    @BeforeEach
    void resetState() {
        mp3FileRepository.deleteAll();

        getS3Keys().forEach(key -> s3TestClient.deleteObject(b -> b.bucket(BUCKET_NAME).key(key)));

        amqpAdmin.purgeQueue(QUEUE_NAME, false);
    }

    @Test
    void checkMP3FileStorage() {
        Long id = mp3StorageService.storeMP3File(mp3Content);

        MP3File storedMP3File = mp3FileRepository.findById(id).orElseThrow();
        byte[] mp3ContentFromS3 = s3TestClient.getObjectAsBytes(b -> b
                .bucket(BUCKET_NAME)
                .key(storedMP3File.getS3Key()))
                .asByteArray();
        assertThat(mp3ContentFromS3).isEqualTo(mp3Content);

        Message idStoredEvent = rabbitTemplate.receive(QUEUE_NAME, 1000);
        assertThat(idStoredEvent).isNotNull();
        String idFromEvent = new String(idStoredEvent.getBody(), UTF_8);
        assertThat(idFromEvent).isEqualTo(id.toString());
    }

    @Test
    void nonMP3FilesAreRejectedWithNoSideEffects() {
        byte[] notMp3 = "nonMP3Content".getBytes(UTF_8);

        assertThatThrownBy(() -> mp3StorageService.storeMP3File(notMp3))
                .isInstanceOf(MP3FileUnparsableException.class);

        assertThat(mp3FileRepository.count()).isZero();
        assertThat(getS3Keys()).isEmpty();
        assertThat(rabbitTemplate.receive(QUEUE_NAME, 1000)).isNull();
    }


    @Test
    void ifMessagingFailsRollbackOfStorageIsPerformed() {
        doThrow(new IllegalStateException())
                .when(resourceStoredEventPublisher).sendResourceStoredEvent(anyLong());

        assertThatThrownBy(() -> mp3StorageService.storeMP3File(mp3Content))
                .isInstanceOf(IllegalStateException.class);

        assertThat(mp3FileRepository.count()).isZero();
        assertThat(getS3Keys()).isEmpty();
        assertThat(rabbitTemplate.receive(QUEUE_NAME, 1000)).isNull();
    }

    @Test
    void checkDeleteMP3Files() {
        Long first = mp3StorageService.storeMP3File(mp3Content);
        Long second = mp3StorageService.storeMP3File(mp3Content);

        List<Long> deleted = mp3StorageService.deleteMP3FilesById(first + "," + second + ",999999");

        assertThat(deleted).containsExactlyInAnyOrder(first, second);
        assertThat(mp3FileRepository.count()).isZero();
        assertThat(getS3Keys()).isEmpty();
        verify(songServiceCallerService).deleteSongMetadata(
                argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(first, second))));
    }

    private static List<String> getS3Keys() {
        return s3TestClient.listObjectsV2(b -> b.bucket(BUCKET_NAME)).contents().stream()
                .map(S3Object::key)
                .toList();
    }
}
