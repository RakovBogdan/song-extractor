package io.github.bohdanrakov.resourceprocessor.services;

import io.github.bohdanrakov.resourceprocessor.dtos.SongServiceStoreRequest;
import io.github.bohdanrakov.resourceprocessor.mappers.TikaMetadataSongRequestMapper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MP3ParserServiceTest {

    private static final String INVALID_MP3_MESSAGE = "The request body is invalid MP3. Only MP3 files are allowed";
    private static final byte[] CONTENT = {1, 2, 3, 4};

    @Mock
    private SongServiceCallerService songServiceCallerService;

    @Mock
    private ResourceServiceCallerService resourceServiceCallerService;

    @Mock
    private TikaMetadataSongRequestMapper tikaMetadataSongRequestMapper;

    @InjectMocks
    private MP3ParserService mp3ParserService;

    @Test
    void processNewResourceUploadsMetadataOfFetchedResource() {
        SongServiceStoreRequest request = new SongServiceStoreRequest(1L, "Song", "Artist", "Album", "03:07", "2026");
        when(resourceServiceCallerService.getMP3ByteContent(1L)).thenReturn(CONTENT);
        when(tikaMetadataSongRequestMapper.toSongRequest(any(Metadata.class), eq(1L))).thenReturn(request);

        try (MockedConstruction<Tika> _ = detecting("audio/mpeg");
             MockedConstruction<Mp3Parser> _ = parsingTitle("Song")) {

            mp3ParserService.processNewResource(1L);
        }

        InOrder order = inOrder(resourceServiceCallerService, tikaMetadataSongRequestMapper, songServiceCallerService);
        order.verify(resourceServiceCallerService).getMP3ByteContent(1L);
        ArgumentCaptor<Metadata> metadata = ArgumentCaptor.forClass(Metadata.class);
        order.verify(tikaMetadataSongRequestMapper).toSongRequest(metadata.capture(), eq(1L));
        order.verify(songServiceCallerService).uploadSongMetadata(request);
        assertThat(metadata.getValue().get("dc:title")).isEqualTo("Song");
    }

    @Test
    void processNewResourceDoesNotUploadInvalidMp3() {
        when(resourceServiceCallerService.getMP3ByteContent(2L)).thenReturn(CONTENT);

        try (MockedConstruction<Tika> _ = detecting("text/plain");
             MockedConstruction<Mp3Parser> _ = mockConstruction(Mp3Parser.class)) {

            assertThatThrownBy(() -> mp3ParserService.processNewResource(2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(INVALID_MP3_MESSAGE);
        }

        verifyNoInteractions(tikaMetadataSongRequestMapper, songServiceCallerService);
    }

    @Test
    void processNewResourcePropagatesResourceServiceFailure() {
        HttpServerErrorException failure = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        when(resourceServiceCallerService.getMP3ByteContent(3L)).thenThrow(failure);

        try (MockedConstruction<Tika> tika = mockConstruction(Tika.class)) {
            assertThatThrownBy(() -> mp3ParserService.processNewResource(3L)).isSameAs(failure);

            assertThat(tika.constructed()).isEmpty();
        }

        verify(songServiceCallerService, never()).uploadSongMetadata(any());
        verifyNoInteractions(tikaMetadataSongRequestMapper);
    }

    @Test
    void extractMetadataReturnsMetadataFilledByParser() throws Exception {
        try (MockedConstruction<Tika> tika = detecting("audio/mpeg");
             MockedConstruction<Mp3Parser> parser = parsingTitle("Song")) {

            Metadata metadata = mp3ParserService.extractMetadata(CONTENT);

            assertThat(metadata.get("dc:title")).isEqualTo("Song");
            verify(tika.constructed().getFirst()).detect(CONTENT);
            ArgumentCaptor<TikaInputStream> stream = ArgumentCaptor.forClass(TikaInputStream.class);
            verify(parser.constructed().getFirst())
                    .parse(stream.capture(), any(ContentHandler.class), eq(metadata), any(ParseContext.class));
            assertThat(stream.getValue()).isNotNull();
        }
    }

    @Test
    void extractMetadataPassesContentToParser() {
        byte[][] parsedContent = new byte[1][];

        try (MockedConstruction<Tika> _ = detecting("audio/mpeg");
             MockedConstruction<Mp3Parser> _ = mockConstruction(Mp3Parser.class, (mock, context) ->
                     doAnswer(invocation -> {
                         parsedContent[0] = invocation.<TikaInputStream>getArgument(0).readAllBytes();
                         return null;
                     }).when(mock).parse(any(), any(), any(), any()))) {

            mp3ParserService.extractMetadata(CONTENT);
        }

        assertThat(parsedContent[0]).isEqualTo(CONTENT);
    }

    @Test
    void extractMetadataRejectsNonMp3MimeTypeWithoutParsing() {
        try (MockedConstruction<Tika> tika = detecting("application/pdf");
             MockedConstruction<Mp3Parser> parser = mockConstruction(Mp3Parser.class)) {

            assertThatThrownBy(() -> mp3ParserService.extractMetadata(CONTENT))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(INVALID_MP3_MESSAGE);

            assertThat(parser.constructed()).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {IOException.class, SAXException.class, TikaException.class})
    void extractMetadataWrapsParserFailure(Class<? extends Exception> failureType) {
        Exception failure = instantiate(failureType);

        try (MockedConstruction<Tika> _ = detecting("audio/mpeg");
             MockedConstruction<Mp3Parser> _ = mockConstruction(Mp3Parser.class, (mock, context) ->
                     doThrow(failure).when(mock).parse(any(), any(), any(), any()))) {

            assertThatThrownBy(() -> mp3ParserService.extractMetadata(CONTENT))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(INVALID_MP3_MESSAGE);
        }
    }

    private static MockedConstruction<Tika> detecting(String mimeType) {
        return mockConstruction(Tika.class, (mock, context) -> when(mock.detect(any(byte[].class))).thenReturn(mimeType));
    }

    private static MockedConstruction<Mp3Parser> parsingTitle(String title) {
        return mockConstruction(Mp3Parser.class, (mock, context) ->
                doAnswer(invocation -> {
                    invocation.<Metadata>getArgument(2).set("dc:title", title);
                    return null;
                }).when(mock).parse(any(), any(), any(), any()));
    }

    private static Exception instantiate(Class<? extends Exception> type) {
        try {
            return type.getConstructor(String.class).newInstance("parser failure");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
