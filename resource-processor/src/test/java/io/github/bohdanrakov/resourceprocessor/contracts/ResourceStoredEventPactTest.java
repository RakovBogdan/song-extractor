package io.github.bohdanrakov.resourceprocessor.contracts;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.github.bohdanrakov.resourceprocessor.consumers.ResourceCreatedConsumer;
import io.github.bohdanrakov.resourceprocessor.services.MP3ParserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "resource-service-messages", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
class ResourceStoredEventPactTest {

    @Pact(consumer = "resource-processor")
    V4Pact resourceStoredEvent(MessagePactBuilder builder) {
        return builder
                .expectsToReceive("a resource stored event")
                .withMetadata(Map.of("contentType", "application/json"))
                .withContent(PactDslJsonRootValue.integerType(1))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "resourceStoredEvent")
    void resourceStoredEventTriggersProcessing(V4Interaction.AsynchronousMessage message) {
        assertThat(message.getContents().getMetadata()).containsEntry("contentType", "application/json");
        MP3ParserService mp3ParserService = mock(MP3ParserService.class);
        ResourceCreatedConsumer consumer = new ResourceCreatedConsumer(mp3ParserService);

        Long resourceId = Long.valueOf(new String(message.contentsAsBytes(), UTF_8));
        consumer.accept(resourceId);

        verify(mp3ParserService).processNewResource(1L);
    }
}
