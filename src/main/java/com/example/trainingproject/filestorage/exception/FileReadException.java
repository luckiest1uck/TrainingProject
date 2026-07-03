package com.example.trainingproject.filestorage.exception;

public final class FileReadException extends FileStorageException {

    public FileReadException(String fileName, Throwable cause) {
        super("Failed to read file: " + fileName, fileName, cause);
    }
}
