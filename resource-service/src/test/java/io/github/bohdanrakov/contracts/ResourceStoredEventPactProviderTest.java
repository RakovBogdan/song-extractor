package io.github.bohdanrakov.contracts;

import au.com.dius.pact.provider.MessageAndMetadata;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import io.github.bohdanrakov.events.ResourceStoredEventPublisher;
import io.github.bohdanrakov.services.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Provider("resource-service-messages")
@PactFolder("../pacts")
@SpringBootTest(properties = "eureka.client.enabled=false")
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class ResourceStoredEventPactProviderTest {

    private static final String DESTINATION = "exchange-resource-id";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:17-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath("../init-scripts/resource-db/init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql");

    @MockitoBean
    private S3Service s3Service;

    @Autowired
    private ResourceStoredEventPublisher resourceStoredEventPublisher;

    @Autowired
    private OutputDestination outputDestination;

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyContract(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @PactVerifyProvider("a resource stored event")
    MessageAndMetadata resourceStoredEvent() {
        resourceStoredEventPublisher.sendResourceStoredEvent(42L);

        Message<byte[]> sent = outputDestination.receive(1000, DESTINATION);
        assertThat(sent).isNotNull();
        return new MessageAndMetadata(sent.getPayload(),
                Map.of("contentType", String.valueOf(sent.getHeaders().get(MessageHeaders.CONTENT_TYPE))));
    }
}
