package com.codice.sra.controllers;

import com.codice.sra.dtos.MatriculaExterna.*;
import com.codice.sra.services.AdmisionOnlineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/admision-online")
@RequiredArgsConstructor
@Tag(name = "Admisión y Matrícula en Línea (Público)",
        description = "Endpoints públicos de autoservicio para aspirantes sin requerimiento de autenticación previa.")
public class AdmisionOnlineController {

    private final AdmisionOnlineService admisionOnlineService;

    // =========================================================================
    // ENDPOINT 1: VERIFICACIÓN PÚBLICA DE IDENTIDAD (Pantalla 1)
    // =========================================================================
    @Operation(
            summary = "Verificar identidad de aspirante",
            description = "Consulta la existencia de la persona en el censo y valida si ya es estudiante activo sin exponer datos sensibles."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verificación ejecutada correctamente.",
                    content = @Content(schema = @Schema(implementation = VerificacionIdentidadPublicaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de consulta inválidos o vacíos.", content = @Content)
    })
    @GetMapping("/verificar-identidad")
    public ResponseEntity<VerificacionIdentidadPublicaResponseDTO> verificarIdentidad(
            @Parameter(description = "ID del tipo de documento (1=DUI, 2=Pasaporte)", example = "1", required = true)
            @RequestParam Long idTipoDocumento,
            @Parameter(description = "Número oficial de documento de identidad", example = "06123456-7", required = true)
            @RequestParam String numeroDocumento) {

        VerificacionIdentidadPublicaResponseDTO response =
                admisionOnlineService.verificarIdentidad(idTipoDocumento, numeroDocumento);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ENDPOINT 2: PRE-MATRÍCULA Y RESERVA DE CUPOS (Pantallas 2 y 3)
    // =========================================================================
    @Operation(
            summary = "Registrar pre-matrícula y apartar cupos",
            description = "Fase 1: Asienta los datos civiles, crea el aspirante provisional (id_usuario = null), reserva cupos en grupos y genera la obligación financiera (arancel de matrícula)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pre-matrícula registrada exitosamente.",
                    content = @Content(schema = @Schema(implementation = MatriculaOnlineResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o formato incorrecto.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflicto por cupos agotados en una o más materias.", content = @Content),
            @ApiResponse(responseCode = "422", description = "No existe ciclo hábil para admisiones en este momento.", content = @Content)
    })
    @PostMapping("/pre-matricular")
    public ResponseEntity<MatriculaOnlineResponseDTO> preMatricular(
            @Valid @RequestBody MatriculaOnlineRequestDTO request) {

        MatriculaOnlineResponseDTO response = admisionOnlineService.procesarPreMatriculaOnline(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // ENDPOINT 3: SIMULACIÓN DE PASARELA Y ACTIVACIÓN IAM (Pantalla 4 - Opción A)
    // =========================================================================
    @Operation(
            summary = "Liquidar matrícula en pasarela simulada y activar credenciales",
            description = "Fase 2: Cobra el arancel con tarjeta, emite factura legal, transiciona matrícula a MATRICULADA y detona la cuenta institucional con rol ESTUDIANTE."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago procesado y cuenta de estudiante creada.",
                    content = @Content(schema = @Schema(implementation = ConfirmacionMatriculaOnlineDTO.class))),
            @ApiResponse(responseCode = "400", description = "Falta cabecera Idempotency-Key o datos de tarjeta inválidos.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Matrícula o cargo no encontrado.", content = @Content),
            @ApiResponse(responseCode = "409", description = "La matrícula ya fue liquidada o cancelada.", content = @Content)
    })
    @PostMapping("/pagar-orden")
    public ResponseEntity<ConfirmacionMatriculaOnlineDTO> pagarOrden(
            @Parameter(description = "Clave de idempotencia única generada por el frontend (UUID)",
                    example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PagoOnlineRequestDTO request) {

        ConfirmacionMatriculaOnlineDTO response =
                admisionOnlineService.liquidarMatriculaOnline(idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ENDPOINT 4: DESISTIMIENTO VOLUNTARIO (Liberación Inmediata de Cupos)
    // =========================================================================
    @Operation(
            summary = "Cancelar solicitud de pre-matrícula voluntariamente",
            description = "Permite al aspirante desistir del trámite antes de pagar, liberando de inmediato los cupos reservados en los grupos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Solicitud cancelada y cupos liberados con éxito.", content = @Content),
            @ApiResponse(responseCode = "400", description = "No se puede cancelar una matrícula ya pagada o con usuario activo.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Matrícula no encontrada.", content = @Content)
    })
    @DeleteMapping("/cancelar/{idMatricula}")
    public ResponseEntity<Void> cancelarPreMatricula(
            @Parameter(description = "ID de la matrícula a cancelar", example = "16", required = true)
            @PathVariable Long idMatricula) {

        admisionOnlineService.cancelarTramiteVoluntario(idMatricula);
        return ResponseEntity.noContent().build();
    }
}