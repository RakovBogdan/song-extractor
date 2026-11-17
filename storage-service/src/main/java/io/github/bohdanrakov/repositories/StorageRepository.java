package io.github.bohdanrakov.repositories;

import io.github.bohdanrakov.models.Storage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StorageRepository extends JpaRepository<Storage, Long> {

    @Query("SELECT s.id FROM Storage s WHERE s.id IN :requestIds")
    List<Long> findExistingIds(List<Long> requestIds);

    boolean existsByBucketAndPath(String bucket, String path);
}
