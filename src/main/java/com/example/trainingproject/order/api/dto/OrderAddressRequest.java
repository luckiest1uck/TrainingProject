package com.example.trainingproject.order.api.dto;

public record OrderAddressRequest(String country, String city, String line, String postcode) {}
