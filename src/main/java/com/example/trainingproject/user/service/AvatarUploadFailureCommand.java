package com.example.trainingproject.user.service;

import org.jspecify.annotations.Nullable;

public record AvatarUploadFailureCommand(
        @Nullable AvatarUploadSourceObject sourceObject,
        @Nullable String failureCode,
        @Nullable String failureMessage) {}
