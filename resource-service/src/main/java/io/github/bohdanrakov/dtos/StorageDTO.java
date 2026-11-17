package io.github.bohdanrakov.dtos;

import io.github.bohdanrakov.models.StorageType;

public record StorageDTO(Long id, StorageType storageType, String bucket, String path) {
}
