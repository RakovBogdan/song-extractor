package io.github.bohdanrakov.controllers;

import io.github.bohdanrakov.dtos.StorageCreateRequest;
import io.github.bohdanrakov.dtos.StorageResponse;
import io.github.bohdanrakov.services.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class StorageServiceController {

    private final StorageService storageService;

    public StorageServiceController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/storages")
    public ResponseEntity<Map<String, Long>> createStorage(
            @Valid @RequestBody StorageCreateRequest storageCreateRequest) {

        Long id = storageService.createStorage(storageCreateRequest);

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("id", id));
    }

    @GetMapping("/storages")
    public ResponseEntity<List<StorageResponse>> getAllStorages() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(storageService.getAllStorages());
    }

    @DeleteMapping("/storages")
    public ResponseEntity<Map<String, List<Long>>> deleteStorages(@RequestParam("id") String ids) {
        List<Long> deletedIds = storageService.deleteStoragesByIds(ids);
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("ids", deletedIds));
    }
}
