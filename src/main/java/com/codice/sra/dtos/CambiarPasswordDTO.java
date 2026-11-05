package com.codice.sra.dtos;
import lombok.Data;
@Data
public class CambiarPasswordDTO {
    private String passwordActual;
    private String nuevaPassword;
    private String confirmarPassword;
}