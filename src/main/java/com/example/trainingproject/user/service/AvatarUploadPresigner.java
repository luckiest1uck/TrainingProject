package com.example.trainingproject.user.service;

import com.example.trainingproject.openapi.dto.AvatarUploadTargetResponse;
import com.example.trainingproject.user.entity.UserAvatarUpload;

public interface AvatarUploadPresigner {

    AvatarUploadTargetResponse presign(UserAvatarUpload upload);
}
