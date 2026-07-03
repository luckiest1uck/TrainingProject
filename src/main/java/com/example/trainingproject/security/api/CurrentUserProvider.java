package com.example.trainingproject.security.api;

import java.util.UUID;

import com.example.trainingproject.security.api.dto.CurrentUserSnapshot;

public interface CurrentUserProvider {

    CurrentUserSnapshot get();

    UUID getUserId();
}
