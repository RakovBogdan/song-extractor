package io.github.bohdanrakov.resourceprocessor.services;


import io.github.bohdanrakov.resourceprocessor.dtos.SongServiceStoreRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;


@Service
public class SongServiceCallerService {

    private static final Logger logger = LoggerFactory.getLogger(SongServiceCallerService.class);
    private final RestClient restClient;

    public SongServiceCallerService(@Qualifier("songsRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class},
            maxRetries = 5,
            delay = 300,
            jitter = 10,
            multiplier = 2,
            maxDelay = 2000
    )
    public void uploadSongMetadata(SongServiceStoreRequest storeRequest) {
        try {
            restClient.post()
                    .uri("/songs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(storeRequest)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict ex) {
            logger.info("Metadata was already loaded for resource {}", storeRequest.id());
        }
    }
}
