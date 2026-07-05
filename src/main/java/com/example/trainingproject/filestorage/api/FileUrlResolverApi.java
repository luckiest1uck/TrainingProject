package com.example.trainingproject.filestorage.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.example.trainingproject.filestorage.api.dto.FileMetadataDto;

public interface FileUrlResolverApi {

    Optional<String> findFileUrl(UUID relatedObjectId);

    Optional<String> findFileUrl(FileMetadataDto fileMetadataDto);

    Map<UUID, String> findFileUrls(List<UUID> relatedObjectIds);
}
