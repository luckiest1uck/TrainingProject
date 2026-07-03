package com.example.trainingproject.user.exception;

import java.util.UUID;

public final class UserAvatarUploadException extends UserException {

    public UserAvatarUploadException(UUID userId, String fileName) {
        super("Avatar upload failed for userId=" + userId + ", fileName=" + fileName);
    }
}
