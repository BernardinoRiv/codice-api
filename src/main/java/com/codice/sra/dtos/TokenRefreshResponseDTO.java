package com.codice.sra.dtos;

public record TokenRefreshResponseDTO(String token, long expiresIn) {}
