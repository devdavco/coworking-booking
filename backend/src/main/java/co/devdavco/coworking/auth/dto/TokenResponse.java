package co.devdavco.coworking.auth.dto;

import java.time.Instant;

public record TokenResponse(String token, Instant expiraEn) {
}
