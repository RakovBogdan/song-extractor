package io.github.bohdanrakov.events;

import io.github.bohdanrakov.exceptions.ResourceStoredEventNotSentExcetpion;
import org.springframework.amqp.AmqpException;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.MessagingException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class ResourceStoredEventPublisher {

    private final StreamBridge streamBridge;

    public ResourceStoredEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Retryable(includes = {MessagingException.class, AmqpException.class, ResourceStoredEventNotSentExcetpion.class},
            maxRetries = 5,
            delay = 300,
            jitter = 10,
            multiplier = 2,
            maxDelay = 2000
    )
    public void sendResourceStoredEvent(Long id) {
        if (!streamBridge.send("id-stored", id)) {
            throw new ResourceStoredEventNotSentExcetpion("Error while sending event to messaging for id: " + id);
        }
    }
}
