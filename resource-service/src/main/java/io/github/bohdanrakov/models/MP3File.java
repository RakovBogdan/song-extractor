package io.github.bohdanrakov.models;


import jakarta.persistence.*;

@Entity
@Table(name = "mp3_file")
public class MP3File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(length = 63)
    private String bucket;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StorageType state;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public StorageType getState() {
        return state;
    }

    public void setState(StorageType state) {
        this.state = state;
    }
}
