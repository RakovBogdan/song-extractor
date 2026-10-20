package io.github.bohdanrakov.resourceprocessor.services;

import io.github.bohdanrakov.resourceprocessor.dtos.SongServiceStoreRequest;
import io.github.bohdanrakov.resourceprocessor.mappers.TikaMetadataSongRequestMapper;
import org.springframework.stereotype.Service;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;


@Service
public class MP3ParserService {

    private final SongServiceCallerService songServiceCallerService;
    private final ResourceServiceCallerService resourceServiceCallerService;
    private final TikaMetadataSongRequestMapper tikaMetadataSongRequestMapper;

    public MP3ParserService(SongServiceCallerService songServiceCallerService, ResourceServiceCallerService resourceServiceCallerService, TikaMetadataSongRequestMapper tikaMetadataSongRequestMapper) {
        this.songServiceCallerService = songServiceCallerService;
        this.resourceServiceCallerService = resourceServiceCallerService;
        this.tikaMetadataSongRequestMapper = tikaMetadataSongRequestMapper;
    }

    public void processNewResource(Long id) {
        byte[] mp3ByteContent = resourceServiceCallerService.getMP3ByteContent(id);
        Metadata metadata = extractMetadata(mp3ByteContent);
        SongServiceStoreRequest songRequest = tikaMetadataSongRequestMapper.toSongRequest(metadata, id);
        songServiceCallerService.uploadSongMetadata(songRequest);
    }

    public Metadata extractMetadata(byte[] mp3content) {
        String mimeType = new Tika().detect(mp3content);
        if (!"audio/mpeg".equals(mimeType)) {
            throw new RuntimeException("The request body is invalid MP3. Only MP3 files are allowed");
        }
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();

        Mp3Parser Mp3Parser = new Mp3Parser();
        try (TikaInputStream inputStream = TikaInputStream.get(mp3content)) {
            Mp3Parser.parse(inputStream, handler, metadata, parseContext);
        } catch (IOException | SAXException | TikaException exception) {
            throw new RuntimeException("The request body is invalid MP3. Only MP3 files are allowed");
        }
        return metadata;
    }
}
