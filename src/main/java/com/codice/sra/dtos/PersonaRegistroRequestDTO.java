package com.codice.sra.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PersonaRegistroRequestDTO extends PersonaBaseRequestDTO {
    // Hereda todos los atributos y validaciones de PersonaBaseRequestDTO
}