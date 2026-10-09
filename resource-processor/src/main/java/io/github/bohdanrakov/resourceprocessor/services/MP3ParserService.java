package io.github.bohdanrakov.resourceprocessor.services;

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


    private void parseMetadata(Metadata metadata) {
        String name = metadata.get("dc:title");
        String artist = metadata.get("xmpDM:artist");
        String album = metadata.get("xmpDM:album");
        String duration = mapDuration(metadata.get("xmpDM:duration"));
        String year = metadata.get("xmpDM:releaseDate");
    }

    private String mapDuration(String duration) {
        double secondsTotal = Double.parseDouble(duration);
        int minutes = (int) secondsTotal / 60;
        int seconds = (int) secondsTotal % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
