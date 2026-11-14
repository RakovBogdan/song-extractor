package io.github.bohdanrakov.resourceprocessor.events;

import io.github.bohdanrakov.resourceprocessor.exceptions.ResourceProcessedEventNotSentException;
import org.springframework.amqp.AmqpException;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.MessagingException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class ResourceProcessedEventPublisher {

    private final StreamBridge streamBridge;

    public ResourceProcessedEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Retryable(includes = {MessagingException.class, AmqpException.class, ResourceProcessedEventNotSentException.class},
            maxRetries = 5,
            delay = 300,
            jitter = 10,
            multiplier = 2,
            maxDelay = 2000
    )
    public void sendResourceProcessedEvent(Long id) {
        if (!streamBridge.send("resource-processed", id)) {
            throw new ResourceProcessedEventNotSentException("Error while sending processed event for id: " + id);
        }
    }
}
