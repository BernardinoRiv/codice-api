package com.codice.sra.dtos;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class NuevoIngresoRequestDTO {

    // 1. Datos de Identidad y Contacto
    private Long idPersona;
    private Long idTipoDocumento;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String telefono;
    private String correoPersonal;
    private Long idDistrito;
    private String direccion;

    // 2. Datos Institucionales y Académicos
    private Long idSede;
    private Long idCarreraSede;
    private Long idPensum;

    // 3. Selección de Horarios
    private List<Long> idGruposSeleccionados;

    // 4. Modalidad Financiera
    // 1 = Ninguna/Regular, 2 = Franja Becaria, 3 = Beca Completa U, 4 = Beca Terceros Directo
    private Integer tipoBeca;
}