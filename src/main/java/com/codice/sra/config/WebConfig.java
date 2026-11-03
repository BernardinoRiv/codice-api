package com.codice.sra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Todo lo que entre a /api/v1/archivos/fotos/ buscará en la carpeta local uploads/fotos_perfil/
        registry.addResourceHandler("/api/v1/archivos/fotos/**")
                .addResourceLocations("file:uploads/fotos_perfil/");
    }
}