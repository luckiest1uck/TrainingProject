package com.example.trainingproject.security.jwt.filter;

record JwtAuthenticationFailure(String typeSlug, String title, String detail, int statusCode, String reasonCode) {}
