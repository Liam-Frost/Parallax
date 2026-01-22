package com.parallax.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.parallax.backend.security.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        JwtProperties.class,
        com.parallax.backend.security.SecurityProperties.class,
        com.parallax.backend.ocr.OcrProperties.class
})
public class ParallaxSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParallaxSpringApplication.class, args);
    }
}
