package com.codice.sra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API SRA - Universidad Modular Abierta")
                        .version("1.0")
                        .description("Documentación de la API REST para el Sistema de Registro Académico (SRA) desarrollado por Códice."));
    }
}