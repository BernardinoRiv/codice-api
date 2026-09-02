# Códice API - Sistema de Registro Académico (SRA)

Backend API para la gestión del Sistema de Registro Académico de la Universidad Modular Abierta (UMA), diseñado por la firma Códice. Este sistema gestiona procesos de matrícula, expedientes académicos y perfiles de usuarios (Administrador, Docente, Estudiante).

## Tecnologías Principales
* **Framework:** Spring Boot 3.x (Java 17)
* **Base de Datos:** PostgreSQL (alojada en Supabase)
* **Seguridad:** JWT (JSON Web Tokens) y Spring Security (RBAC)
* **Construcción y Despliegue:** Maven y Docker

## Requisitos Previos
Para levantar este proyecto en un entorno local, necesitas tener instalado:
* JDK 17
* Maven 3.8+
* Tu propio archivo `.env` o credenciales válidas en `application.properties`

## Ejecución Local
1. Clona el repositorio.
2. Asegúrate de configurar las variables de entorno de la base de datos.
3. Ejecuta el comando: `mvn spring-boot:run`