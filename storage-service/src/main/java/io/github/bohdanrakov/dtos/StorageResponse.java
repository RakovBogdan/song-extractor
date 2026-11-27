package io.github.bohdanrakov.dtos;

import io.github.bohdanrakov.models.StorageType;

public record StorageResponse(Long id, StorageType storageType, String bucket, String path) {
}
