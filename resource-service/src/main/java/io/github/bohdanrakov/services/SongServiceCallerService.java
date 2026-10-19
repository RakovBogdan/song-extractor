package io.github.bohdanrakov.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SongServiceCallerService {

    private static final Logger logger = LoggerFactory.getLogger(SongServiceCallerService.class);
    private final RestClient restClient;

    public SongServiceCallerService(RestClient restClient) {
        this.restClient = restClient;
    }

    public void deleteSongMetadata(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }

        String idsString = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        try {
            restClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/songs")
                            .queryParam("id", idsString)
                            .build())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            logger.error("Failed to call song-service delete metadata", ex);
            throw ex;
        }
    }
}
