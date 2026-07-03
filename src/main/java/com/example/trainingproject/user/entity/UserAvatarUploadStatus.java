package com.example.trainingproject.user.entity;

public enum UserAvatarUploadStatus {
    PENDING_UPLOAD,
    @SuppressWarnings("unused")
    UPLOADED,
    PROCESSING,
    READY,
    FAILED,
    EXPIRED,
    SUPERSEDED
}
