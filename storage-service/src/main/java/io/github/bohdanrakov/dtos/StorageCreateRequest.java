package io.github.bohdanrakov.dtos;

import io.github.bohdanrakov.models.StorageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StorageCreateRequest(
        @NotNull(message = "Storage type is required")
        StorageType storageType,

        @NotNull(message = "Bucket is required")
        @Pattern(regexp = "^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$",
                message = "Bucket must be 3-63 characters long and contain only lowercase letters, digits, "
                        + "dots and hyphens, starting and ending with a letter or digit")
        String bucket,

        @NotNull(message = "Path is required")
        @Size(min = 1, max = 255, message = "Path must be between 1 and 255 characters")
        @Pattern(regexp = "^/.*$", message = "Path must start with '/'")
        String path
) {
}
