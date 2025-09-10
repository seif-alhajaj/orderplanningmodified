package com.pcagrade.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

      // Authorized URLs for your frontend
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",   // All localhost ports
                "http://127.0.0.1:*",     // All ports 127.0.0.1
                "https://localhost:*"    // HTTPS localhost if needed
        ));

      // Allowed HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

       // Authorized headers
        config.setAllowedHeaders(Arrays.asList("*"));
       // No credentials to simplify
        config.setAllowCredentials(false);

      // Cache preflight requests
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
