package io.github.bohdanrakov.services;

import io.github.bohdanrakov.dtos.MP3FileDTO;
import io.github.bohdanrakov.events.ResourceStoredEventPublisher;
import io.github.bohdanrakov.exceptions.IdListTooLargeException;
import io.github.bohdanrakov.exceptions.InvalidIdException;
import io.github.bohdanrakov.exceptions.MP3FileNotFoundException;
import io.github.bohdanrakov.exceptions.MP3FileUnparsableException;
import io.github.bohdanrakov.mappers.MP3FileMapper;
import io.github.bohdanrakov.models.MP3File;
import io.github.bohdanrakov.repositories.MP3FileRepository;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MP3StorageService {

    private static final Logger logger = LoggerFactory.getLogger(MP3StorageService.class);

    private final MP3FileRepository mp3FileRepository;
    private final MP3FileMapper mp3FileMapper;
    private final SongServiceCallerService songServiceCallerService;
    private final S3Service s3Service;
    private final ResourceStoredEventPublisher resourceStoredEventPublisher;

    public MP3StorageService(MP3FileRepository mp3FileRepository, MP3FileMapper mp3FileMapper,
                             SongServiceCallerService songServiceCallerService,
                             S3Service s3Service, ResourceStoredEventPublisher resourceStoredEventPublisher) {
        this.mp3FileRepository = mp3FileRepository;
        this.mp3FileMapper = mp3FileMapper;
        this.songServiceCallerService = songServiceCallerService;
        this.s3Service = s3Service;
        this.resourceStoredEventPublisher = resourceStoredEventPublisher;
    }

    public Long storeMP3File(byte[] mp3content) {
        validateMp3Content(mp3content);

        String s3key = UUID.randomUUID().toString();
        s3Service.storeMP3File(s3key, mp3content);
        MP3File mp3File = new MP3File();
        mp3File.setS3Key(s3key);
        MP3File savedMP3File;
        try {
            savedMP3File = mp3FileRepository.save(mp3File);
        } catch (RuntimeException ex) {
            s3Service.deleteMp3Files(List.of(s3key));
            throw ex;
        }
        try {
            resourceStoredEventPublisher.sendResourceStoredEvent(savedMP3File.getId());
        } catch (RuntimeException ex) {
            rollbackStorage(ex, s3key, savedMP3File.getId());
        }

        return savedMP3File.getId();
    }

    private void rollbackStorage(RuntimeException messagingException, String s3Key, Long id) {
        try {
            mp3FileRepository.deleteById(id);
        } catch (RuntimeException ex) {
            messagingException.addSuppressed(ex);
        }

        try {
            s3Service.deleteMp3Files(List.of(s3Key));
        } catch (RuntimeException ex) {
            messagingException.addSuppressed(ex);
        }
    }

    private void validateMp3Content(byte[] mp3content) {
        String mimeType = new Tika().detect(mp3content);
        if (!"audio/mpeg".equals(mimeType)) {
            throw new MP3FileUnparsableException("The request body is invalid MP3. Only MP3 files are allowed");
        }
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();

        Mp3Parser Mp3Parser = new Mp3Parser();
        try (TikaInputStream inputStream = TikaInputStream.get(mp3content)) {
            Mp3Parser.parse(inputStream, handler, metadata, parseContext);
        } catch (IOException | SAXException | TikaException exception) {
            logger.error(exception.getMessage(), exception);
            throw new MP3FileUnparsableException("The request body is invalid MP3. Only MP3 files are allowed");
        }
    }

    public MP3FileDTO getMP3File(long id) {
        if (id <= 0) {
            throw new InvalidIdException("Invalid value '" + id + "' for ID. Must be a positive integer");
        }
        Optional<MP3File> mp3File = mp3FileRepository.findById(id);
        if (mp3File.isEmpty()) {
            throw new MP3FileNotFoundException("Resource with ID=" + id + " not found");
        }
        byte[] mp3FileContent = s3Service.getMP3FileContent(mp3File.get().getS3Key());
        return mp3FileMapper.toDto(mp3File.get(), mp3FileContent);
    }

    @Transactional
    public List<Long> deleteMP3FilesById(String ids) {
        if (ids.length() > 200) {
            throw new IdListTooLargeException("CSV string is too long: received " + ids.length()
                    + " characters, maximum allowed is 200");
        }

        List<Long> parsedIds = new ArrayList<>();

        for (String id : ids.split(",")) {
            if (!isValidLong(id)) {
                throw new InvalidIdException("Invalid ID format: '" + id + "'. Only positive integers are allowed");
            }
            parsedIds.add(Long.parseLong(id));
        }

        List<MP3File> mp3Files = mp3FileRepository.findAllById(parsedIds);
        List<Long> idsForDeletion = mp3Files.stream().map(MP3File::getId).toList();
        List<String> s3KeysForDeletion = mp3Files.stream().map(MP3File::getS3Key).toList();

        mp3FileRepository.deleteAllById(idsForDeletion);
        songServiceCallerService.deleteSongMetadata(idsForDeletion);
        s3Service.deleteMp3Files(s3KeysForDeletion);
        return idsForDeletion;
    }

    private boolean isValidLong(String number) {
        for (int i = 0; i < number.length(); i++) {
            if(!Character.isDigit(number.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
