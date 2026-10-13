package com.udea.orquestador.infraestructura;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class ConfiguracionCors {
    @Bean
    public CorsFilter filtroCors(@Value("${app.cors.allowed-origin:http://localhost:5173}") String origenes,
            @Value("${DEMO_CORS_ALL_ORIGINS:false}") boolean demoAbierto) {
        CorsConfiguration cors = new CorsConfiguration();
        if (demoAbierto) {
            cors.setAllowedOriginPatterns(List.of("*"));
        } else {
            cors.setAllowedOrigins(Arrays.stream(origenes.split(","))
                    .map(String::trim).filter(origen -> !origen.isBlank()).toList());
        }
        cors.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("Content-Disposition", "Location", "X-Request-Id"));
        cors.setAllowCredentials(!demoAbierto);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource origen = new UrlBasedCorsConfigurationSource();
        origen.registerCorsConfiguration("/**", cors);
        return new CorsFilter(origen);
    }
}
