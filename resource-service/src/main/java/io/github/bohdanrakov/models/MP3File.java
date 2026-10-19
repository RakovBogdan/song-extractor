package io.github.bohdanrakov.models;


import jakarta.persistence.*;

@Entity
@Table(name = "mp3_file")
public class MP3File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "s3_key")
    private String s3Key;

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
}
