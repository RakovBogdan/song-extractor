package io.github.bohdanrakov.resourceprocessor.consumers;

import io.github.bohdanrakov.resourceprocessor.services.MP3ParserService;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component("resourceCreatedConsumer")
public class ResourceCreatedConsumer implements Consumer<Long> {

    private final MP3ParserService mp3ParserService;

    public ResourceCreatedConsumer(MP3ParserService mp3ParserService) {
        this.mp3ParserService = mp3ParserService;
    }

    @Override
    public void accept(Long resourceId) {
        mp3ParserService.processNewResource(resourceId);
    }
}
