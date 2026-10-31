package co.devdavco.coworking.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> origins) {

    public CorsProperties {
        origins = origins == null ? List.of() : origins.stream()
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();
    }
}
