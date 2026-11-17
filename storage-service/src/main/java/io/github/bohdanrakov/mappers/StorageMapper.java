package io.github.bohdanrakov.mappers;

import io.github.bohdanrakov.dtos.StorageCreateRequest;
import io.github.bohdanrakov.dtos.StorageResponse;
import io.github.bohdanrakov.models.Storage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StorageMapper {

    @Mapping(target = "id", ignore = true)
    Storage toEntity(StorageCreateRequest storageCreateRequest);

    StorageResponse toDto(Storage storage);

    List<StorageResponse> toDtos(List<Storage> storages);
}
