package com.codice.sra.enums;

public enum TipoCatalogo {
    ROLES("roles", "id_rol", "rol"),
    ESTADOS_REGISTRO_PERSONA("estados_registro_persona", "id_estado_registro", "estado_registro"),
    TIPOS_DOCUMENTO("tipos_documento", "id_tipo_documento", "tipo_documento"),
    ESTADOS_USUARIO("estados_usuario", "id_estado_usuario", "estado_usuario"),
    TIPOS_SEDE("tipos_sede", "id_tipo_sede", "tipo_sede"),
    ESTADOS_SEDE("estados_sede", "id_estado_sede", "estado_sede"),
    ESTADOS_FACULTAD("estados_facultad", "id_estado_facultad", "estado_facultad"),
    ESTADOS_EDIFICIO("estados_edificio", "id_estado_edificio", "estado_edificio"),
    TIPOS_AULA("tipos_aula", "id_tipo_aula", "tipo_aula"),
    ESTADOS_AULA("estados_aula", "id_estado_aula", "estado_aula"),
    CARGOS("cargos", "id_cargo", "cargo"),
    ESTADOS_EMPLEADO("estados_empleado", "id_estado_empleado", "estado_empleado"),
    NIVELES_ACADEMICOS("niveles_academicos", "id_nivel", "nivel"),
    ESTADOS_CARRERA("estados_carrera", "id_estado_carrera", "estado_carrera"),
    ESTADOS_PENSUM("estados_pensum", "id_estado_pensum", "estado_pensum"),
    TIPOS_CONTRATACION_DOCENTE("tipos_contratacion_docente", "id_tipo_contratacion", "tipo_contratacion"),
    ESTADOS_DOCENTE("estados_docente", "id_estado_docente", "estado_docente"),
    ESTADOS_ESTUDIANTE("estados_estudiante", "id_estado_estudiante", "estado_estudiante"),
    ESTADOS_TRAYECTORIA("estados_trayectoria", "id_estado_trayectoria", "estado_trayectoria"),
    ESTADOS_CICLO("estados_ciclo", "id_estado_ciclo", "estado_ciclo"),
    TIPOS_PERIODO("tipos_periodo", "id_tipo_periodo", "tipo_periodo"),
    ESTADOS_GRUPO("estados_grupo", "id_estado_grupo", "estado_grupo"),
    MODALIDADES("modalidades", "id_modalidad", "modalidad"),
    DIAS_SEMANA("dias_semana", "id_dia", "dia"),
    ESTADOS_MATRICULA("estados_matricula", "id_estado_matricula", "estado_matricula"),
    ESTADOS_INSCRIPCION("estados_inscripcion", "id_estado_inscripcion", "estado_inscripcion"),
    ESTADOS_MATERIA_ESTUDIANTE("estados_materia_estudiante", "id_estado_materia", "estado_materia"),
    ESTADOS_ASISTENCIA("estados_asistencia", "id_estado_asistencia", "estado_asistencia"),
    TIPOS_EVALUACION("tipos_evaluacion", "id_tipo_evaluacion", "tipo_evaluacion"),
    ESTADOS_CALIFICACION("estados_calificacion", "id_estado_calificacion", "estado_calificacion"),
    ESTADOS_SOLICITUD("estados_solicitud", "id_estado_solicitud", "estado_solicitud"),
    TIPOS_COBRO("tipos_cobro", "id_tipo_cobro", "tipo_cobro"),
    ESTADOS_RECARGO("estados_recargo", "id_estado_recargo", "estado_recargo"),
    ESTADOS_BENEFICIO("estados_beneficio", "id_estado_beneficio", "estado_beneficio"),
    ESTADOS_BENEFICIO_ESTUDIANTE("estados_beneficio_estudiante", "id_estado_beneficio_estudiante", "estado_beneficio_estudiante"),
    ESTADOS_CARGO("estados_cargo", "id_estado_cargo", "estado_cargo"),
    METODOS_PAGO("metodos_pago", "id_metodo_pago", "metodo_pago"),
    CANALES_PAGO("canales_pago", "id_canal_pago", "canal_pago"),
    ESTADOS_PAGO("estados_pago", "id_estado_pago", "estado_pago"),
    ESTADOS_FACTURA("estados_factura", "id_estado_factura", "estado_factura"),
    CICLOS("ciclos", "id_ciclo", "codigo_ciclo"),
    SEDES("sedes", "id_sede", "nombre_sede"),
    CARRERAS("carreras", "id_carrera", "nombre_carrera");

    private final String tabla;
    private final String columnaId;
    private final String columnaNombre;

    TipoCatalogo(String tabla, String columnaId, String columnaNombre) {
        this.tabla = tabla;
        this.columnaId = columnaId;
        this.columnaNombre = columnaNombre;
    }

    public String getTabla() { return tabla; }
    public String getColumnaId() { return columnaId; }
    public String getColumnaNombre() { return columnaNombre; }

    public static TipoCatalogo fromSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("El identificador del catálogo no puede estar vacío");
        }
        String normalizado = slug.trim().replace("-", "_").toUpperCase();
        for (TipoCatalogo tipo : values()) {
            if (tipo.name().equals(normalizado)) {
                return tipo;
            }
        }
        // Dispara la excepción que tu GlobalExceptionHandler ya sabe procesar
        throw new IllegalArgumentException("El catálogo '" + slug + "' no existe o no está registrado");
    }
}