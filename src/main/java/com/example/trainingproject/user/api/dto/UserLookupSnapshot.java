package com.example.trainingproject.user.api.dto;

import java.util.UUID;

public record UserLookupSnapshot(UUID id, String firstName, String lastName, String email) {}
