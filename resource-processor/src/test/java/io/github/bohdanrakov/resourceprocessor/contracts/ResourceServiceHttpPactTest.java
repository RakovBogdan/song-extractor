package io.github.bohdanrakov.resourceprocessor.contracts;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.github.bohdanrakov.resourceprocessor.services.ResourceServiceCallerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "resource-service", pactVersion = PactSpecVersion.V4)
class ResourceServiceHttpPactTest {

    private static final byte[] SAMPLE_MP3 = readSampleMp3();

    @Pact(consumer = "resource-processor")
    V4Pact existingResource(PactDslWithProvider builder) {
        return builder
                .given("a resource exists")
                .uponReceiving("a request for an existing resource")
                    .pathFromProviderState("/resources/${id}", "/resources/1")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .withBinaryData(SAMPLE_MP3, "audio/mpeg")
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "resource-processor")
    V4Pact missingResource(PactDslWithProvider builder) {
        return builder
                .given("no resource exists")
                .uponReceiving("a request for a missing resource")
                    .path("/resources/999999")
                    .method("GET")
                .willRespondWith()
                    .status(404)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "existingResource")
    void fetchesMp3ContentOfExistingResource(MockServer mockServer) {
        ResourceServiceCallerService client = new ResourceServiceCallerService(RestClient.create(mockServer.getUrl()));

        byte[] content = client.getMP3ByteContent(1L);

        assertThat(content).isEqualTo(SAMPLE_MP3);
    }

    @Test
    @PactTestFor(pactMethod = "missingResource")
    void failsWithNotFoundForMissingResource(MockServer mockServer) {
        ResourceServiceCallerService client = new ResourceServiceCallerService(RestClient.create(mockServer.getUrl()));

        assertThatThrownBy(() -> client.getMP3ByteContent(999999L))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    private static byte[] readSampleMp3() {
        try (InputStream in = ResourceServiceHttpPactTest.class.getResourceAsStream("/sample.mp3")) {
            return Objects.requireNonNull(in).readAllBytes();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
