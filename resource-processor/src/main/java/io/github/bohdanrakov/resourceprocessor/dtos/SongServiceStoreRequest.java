package io.github.bohdanrakov.resourceprocessor.dtos;

public record SongServiceStoreRequest(Long id, String name, String artist, String album, String duration, String year) {
}
