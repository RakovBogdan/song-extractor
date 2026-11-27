package io.github.bohdanrakov.events;

import io.github.bohdanrakov.services.MP3StorageService;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component("resourceProcessedConsumer")
public class ResourceProcessedConsumer implements Consumer<Long> {

    private final MP3StorageService mp3StorageService;

    public ResourceProcessedConsumer(MP3StorageService mp3StorageService) {
        this.mp3StorageService = mp3StorageService;
    }

    @Override
    public void accept(Long resourceId) {
        mp3StorageService.moveToPermanentStorage(resourceId);
    }
}
