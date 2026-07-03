package com.example.trainingproject.filestorage.api;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.example.trainingproject.filestorage.api.dto.FileMetadataDto;

public interface FileStorageWriterApi {

    boolean isEnabled();

    void store(MultipartFile file, FileMetadataDto fileMetadataDto);

    void recordExisting(FileMetadataDto fileMetadataDto);

    void enqueueDeleteObject(FileMetadataDto fileMetadataDto);

    void deleteFile(UUID relatedObjectId);
}
