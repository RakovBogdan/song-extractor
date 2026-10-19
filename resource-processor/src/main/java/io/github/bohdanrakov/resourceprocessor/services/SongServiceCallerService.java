package io.github.bohdanrakov.resourceprocessor.services;


import io.github.bohdanrakov.resourceprocessor.dtos.SongServiceStoreRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


@Service
public class SongServiceCallerService {

    private static final Logger logger = LoggerFactory.getLogger(SongServiceCallerService.class);
    private final RestClient restClient;

    public SongServiceCallerService(@Qualifier("songsRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void uploadSongMetadata(SongServiceStoreRequest storeRequest) {
        try {
            restClient.post()
                    .uri("/songs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(storeRequest)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            logger.error("Failed to call song-service store metadata", ex);
            throw ex;
        }
    }
}
