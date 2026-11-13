CREATE TABLE storage (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    storage_type VARCHAR(20) NOT NULL CHECK (storage_type IN ('STAGING', 'PERMANENT')),
    bucket VARCHAR(63) NOT NULL,
    path VARCHAR(255) NOT NULL,
    CONSTRAINT uq_storage_bucket_path UNIQUE (bucket, path)
);

INSERT INTO storage (storage_type, bucket, path) VALUES
    ('STAGING', 'staging-mp3-storage', '/files'),
    ('PERMANENT', 'permanent-mp3-storage', '/files');
