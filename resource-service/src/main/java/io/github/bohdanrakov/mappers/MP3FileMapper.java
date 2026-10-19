package io.github.bohdanrakov.mappers;

import io.github.bohdanrakov.dtos.MP3FileDTO;
import io.github.bohdanrakov.models.MP3File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MP3FileMapper {

    @Mapping(target = "byteContent", source = "content")
    MP3FileDTO toDto(MP3File mp3File, byte[] content);
}
