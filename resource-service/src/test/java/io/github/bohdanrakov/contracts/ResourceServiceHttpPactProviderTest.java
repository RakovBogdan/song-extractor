package io.github.bohdanrakov.contracts;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import io.github.bohdanrakov.models.MP3File;
import io.github.bohdanrakov.repositories.MP3FileRepository;
import io.github.bohdanrakov.services.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import static org.mockito.Mockito.when;

@Provider("resource-service")
@PactFolder("../pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class ResourceServiceHttpPactProviderTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:17-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath("../init-scripts/resource-db/init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql");

    @MockitoBean
    private S3Service s3Service;

    @LocalServerPort
    private int port;

    @Autowired
    private MP3FileRepository mp3FileRepository;

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyContract(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("a resource exists")
    Map<String, Object> resourceExists() {
        MP3File mp3File = new MP3File();
        mp3File.setS3Key("pact-sample");
        MP3File saved = mp3FileRepository.save(mp3File);
        when(s3Service.getMP3FileContent("pact-sample")).thenReturn(readSampleMp3());
        return Map.of("id", saved.getId());
    }

    @State("no resource exists")
    void noResourceExists() {
        mp3FileRepository.deleteAll();
    }

    private static byte[] readSampleMp3() {
        try (InputStream in = ResourceServiceHttpPactProviderTest.class.getResourceAsStream("/sample.mp3")) {
            return Objects.requireNonNull(in).readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
