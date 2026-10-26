package io.github.bohdanrakov.resourceprocessor.mappers;

import io.github.bohdanrakov.resourceprocessor.dtos.SongServiceStoreRequest;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Component;

@Component
public class TikaMetadataSongRequestMapper {

    public SongServiceStoreRequest toSongRequest(Metadata metadata, Long id) {
        String name = metadata.get("dc:title");
        String artist = metadata.get("xmpDM:artist");
        String album = metadata.get("xmpDM:album");
        String duration = mapDuration(metadata.get("xmpDM:duration"));
        String year = metadata.get("xmpDM:releaseDate");

        return new SongServiceStoreRequest(id, name, artist, album, duration, year);
    }

    private String mapDuration(String duration) {
        double secondsTotal = Double.parseDouble(duration);
        int minutes = (int) secondsTotal / 60;
        int seconds = (int) secondsTotal % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
