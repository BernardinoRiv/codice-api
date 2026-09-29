# Códice API - Sistema de Registro Académico (SRA)

Backend API para la gestion del Sistema de Registro Académico de la Universidad Modular Abierta (UMA), diseñado por la firma Códice. Este sistema gestiona procesos de matrícula, expedientes académicos, perfiles de usuarios (Administrador, Docente, Estudiante, Finanzas, Registro Académico) y mecanismos avanzados de seguridad.

## Tecnologías principales
* **Framework:** Spring Boot 3.x (Java 17)
* **Base de Datos:** PostgreSQL (alojada en Supabase)
* **Seguridad:** JWT (JSON Web Tokens), Spring Security (RBAC) y validación de sesiones activas
* **Construcción y Despliegue:** Maven y Docker
* **Documentación API:** Swagger / OpenAPI 3
* **Notificaciones:** JavaMailSender para alertas de seguridad

## Requisitos previos
Para levantar este proyecto en un entorno local, necesitas tener instalado:
* JDK 17
* Maven 3.8+
* Tu propio archivo `.env` o credenciales válidas en `application.properties`

## Estructura del proyecto
```
src/main/java/com/codice/sra/
├── config/          # Configuración de seguridad y CORS
├── controllers/     # Endpoints REST de cada módulo
├── dtos/            # Objetos de transferencia de datos
├── enums/           # Enumeraciones (roles, estados)
├── exceptions/      # Manejo global de excepciones
├── models/          # Entidades JPA
├── repositories/    # Interfaces de acceso a datos
├── security/        # Filtro JWT y servicio de tokens
├── services/        # Lógica de negocio
└── utils/           # Utilidades generales
```

## Módulos implementados

### 1. Autenticación y seguridad
* **Login institucional:** Validación de credenciales con correo institucional y contraseña encriptada (BCrypt).
* **Bloqueo por intentos fallidos:** Tras 3 intentos consecutivos fallidos, la cuenta se bloquea automáticamente por 15 minutos.
* **Forzar cambio de contraseña:** El primer acceso requiere modificar la contraseña temporal asignada por el administrador.
* **Tokens JWT:** Generación de tokens con claims personalizados (`idUsuario`, `rol`, `idSesion`).

### 2. Gestión de sesiones y trazabilidad
* **Registro automático de sesiones:** Cada inicio de sesión exitoso queda registrado con dirección IP y User-Agent del cliente.
* **Detección de anomalías:** El sistema identifica automáticamente cuando un usuario accede desde un dispositivo o dirección IP no habitual.
* **Alertas de seguridad por correo:** Notificación automática al usuario cuando se detecta un acceso inusual.
* **Historial de sesiones:** El usuario puede consultar todas sus sesiones activas y finalizadas desde la sección de Ajustes.
* **Cierre de sesiones:** Permite cerrar una sesión específica o todas las sesiones activas desde cualquier dispositivo.
* **Invalidación en tiempo real:** Al cerrar una sesión, el filtro de seguridad (`JwtAuthenticationFilter`) rechaza inmediatamente las peticiones posteriores con código 401.

### 3. Gestión académica
* **Ingreso individual de calificaciones:** Captura y actualización de notas por actividad evaluativa de forma individual.
* **Carga masiva de calificaciones:** Importación de notas mediante plantillas Excel con validaciones estrictas de formato.
* **Modificación de notas:** Edición de calificaciones previamente registradas antes del cierre oficial del período.
* **Evaluaciones:** Soporte para laboratorios y parciales con períodos de apertura y cierre configurables.
* **Creación de grupos:** Asignación de materias docentes y aulas a un grupo.
* **Validaciones de solapamiento:** Validar que no exista conflictos de horarios, docentes y aulas.


### 4. Gestión de usuarios
* **Registro de docentes:** Alta de nuevos docentes con datos personales, sede, tipo de contratación y especialidad.
* **Registro de empleados:** Alta de personal administrativo con área y cargo asignado.
* **Roles disponibles:** DOCENTE, ESTUDIANTE, ADMINISTRADOR, FINANZAS, REGISTRO_ACADEMICO.
* **Busquedas Avanzadas:** Consulta previa de datos biográficos por número de documento con bloqueo pesimista.
* **Aprovisionamiento de cuentas:** Aprovisionar de una cuenta para usuario del sistema a docentes y empleados.


## Endpoints principales

### Autenticación (`/api/v1/auth`)
* `POST /login` - Inicio de sesión institucional.
* `POST /cambiar-contrasena` - Modificación de contraseña (requiere autenticación).

### Sesiones (`/api/v1/sesiones`)
* `GET /historial` - Lista las sesiones del usuario autenticado.
* `POST /registrar` - Registra una nueva sesión (invocado automáticamente tras el login).
* `POST /{idSesion}/cerrar` - Cierra una sesión específica.
* `POST /cerrar-todas` - Cierra todas las sesiones activas del usuario.

### Docentes (`/api/v1/docentes`)
* `POST /` - Registro de nuevo docente.
* `GET /grupos` - Consulta de grupos asignados al docente autenticado.

### Empleados (`/api/v1/empleados`)
* `POST /` - Registro de nuevo empleado.

### Calificaciones (`/api/v1/calificaciones`)
* `POST /carga-masiva/{idGrupo}` - Carga masiva de notas mediante Excel.
* `POST /individual` - Registro individual de una calificación.
* `PUT /{idCalificacion}` - Actualización de una calificación existente.

## Seguridad

El sistema implementa un esquema de seguridad en capas:

1. **Filtro JWT (`JwtAuthenticationFilter`):** Valida el token en cada petición y verifica que la sesión asociada siga activa en la base de datos.
2. **Control de acceso basado en roles (RBAC):** Los endpoints están protegidos con `@PreAuthorize` para restringir el acceso según el rol del usuario.
3. **Validación de sesiones activas:** Si un administrador o el propio usuario cierra una sesión desde otro dispositivo, las peticiones posteriores son rechazadas con código 401.
4. **CORS configurado:** Permite conexiones desde los dominios autorizados del frontend.

## Ejecución Local

1. Clona el repositorio.
2. Asegúrate de configurar las variables de entorno de la base de datos en `application.properties`:
    * `spring.datasource.url`
    * `spring.datasource.username`
    * `spring.datasource.password`
    * `security.jwt.secret-key`
    * `security.jwt.expiration-time`
    * `spring.mail.*` (para el envío de alertas)
3. Ejecuta el comando: `mvn spring-boot:run`
4. La API estará disponible en `http://localhost:8080`
5. La documentación Swagger en `http://localhost:8080/swagger-ui.html`

## Equipo de Desarrollo

Firma Códice - Desarrollo de software a medida para instituciones educativas.
