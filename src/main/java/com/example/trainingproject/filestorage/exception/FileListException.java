package com.example.trainingproject.filestorage.exception;

public final class FileListException extends FileStorageException {

    public FileListException(String bucketName, Throwable cause) {
        super("Failed to list files in bucket: " + bucketName, bucketName, cause);
    }
}
