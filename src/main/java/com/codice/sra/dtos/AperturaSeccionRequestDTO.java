package com.codice.sra.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AperturaSeccionRequestDTO {

    @NotNull(message = "El identificador del ciclo lectivo es obligatorio.")
    @Positive(message = "El idCiclo debe ser un identificador válido.")
    private Long idCiclo;

    @NotNull(message = "El identificador de la sede es obligatorio.")
    @Positive(message = "El idSede debe ser un identificador válido.")
    private Long idSede;

    @NotNull(message = "El identificador de la materia es obligatorio.")
    @Positive(message = "El idMateria debe ser un identificador válido.")
    private Long idMateria;

    @NotNull(message = "El identificador del docente es obligatorio.")
    @Positive(message = "El idDocente debe ser un identificador válido.")
    private Long idDocente;

    @NotNull(message = "Debe seleccionar una plantilla horaria institucional.")
    @Positive(message = "El idPlantilla debe ser un identificador válido.")
    private Long idPlantilla;

    @NotNull(message = "La modalidad de impartición es obligatoria.")
    @Positive(message = "El idModalidad debe ser un identificador válido.")
    private Long idModalidad;

    // Opcionales según la modalidad (se validan a nivel de servicio)
    private Long idAula;

    private String enlaceVirtual;

    private Integer cupoVirtual;
}