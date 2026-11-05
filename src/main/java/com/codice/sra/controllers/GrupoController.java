package com.codice.sra.controllers;

import com.codice.sra.dtos.*;
import com.codice.sra.services.GrupoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/grupos")
@RequiredArgsConstructor
@Tag(name = "Gestión de Grupos", description = "Endpoints para la gestión de oferta académica, secciones, calificaciones y evaluaciones.")
public class GrupoController {

    private final GrupoService grupoService;

    // ENDPOINT NUEVO: APERTURA DE SECCIÓN POR PLANTILLA INSTITUCIONAL
    @PostMapping("/aperturar")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(
            summary = "Aperturar una sección académica por plantilla",
            description = "Crea una nueva sección académica vinculada a una materia, ciclo, sede y docente a partir de una plantilla horaria oficial, clonando sus bloques temporales y validando aforo y cruces de horario."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sección aperturada exitosamente con sus horarios clonados"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada con formato inválido"),
            @ApiResponse(responseCode = "404", description = "Catálogo referenciado no encontrado o inactivo"),
            @ApiResponse(responseCode = "409", description = "Conflicto por traslape de horario (docente/aula) o concurrencia"),
            @ApiResponse(responseCode = "422", description = "Violación de regla de negocio (límite laboral, aula fuera de sede, código duplicado)")
    })
    public ResponseEntity<SeccionResponseDTO> aperturarSeccion(
            @Valid @RequestBody AperturaSeccionRequestDTO request) {

        SeccionResponseDTO nuevaSeccion = grupoService.aperturarSeccionPorPlantilla(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(nuevaSeccion.getIdGrupo())
                .toUri();

        return ResponseEntity.created(location).body(nuevaSeccion);
    }

    @GetMapping("/catalogos/plantillas")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Obtener catálogo de plantillas horarias institucionales con franjas temporales")
    public ResponseEntity<List<PlantillaHorarioResponseDTO>> obtenerPlantillasHorarias() {
        return ResponseEntity.ok(grupoService.obtenerPlantillasActivas());
    }

    @GetMapping("/catalogos/aulas")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Obtener aulas físicas disponibles por sede")
    public ResponseEntity<List<AulaResponseDTO>> obtenerAulasPorSede(@RequestParam Long idSede) {
        return ResponseEntity.ok(grupoService.obtenerAulasPorSede(idSede));
    }

    @GetMapping("/catalogos/materias")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Obtener materias del pensum activo ordenadas por ciclo recomendado")
    public ResponseEntity<List<MateriaPensumResponseDTO>> obtenerMateriasPorCarrera(@RequestParam Long idCarrera) {
        return ResponseEntity.ok(grupoService.obtenerMateriasPorCarrera(idCarrera));
    }

    @GetMapping("/catalogos/docentes")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Listado ligero de docentes activos filtrados por sede para asignación de grupos")
    public ResponseEntity<List<DocenteSeleccionDTO>> obtenerDocentesParaSeleccion(
            @RequestParam(name = "idSede", required = false) Long idSede) {
        return ResponseEntity.ok(grupoService.obtenerDocentesParaSeleccion(idSede));
    }

    @GetMapping("/{idGrupo}/inscripciones")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('DOCENTE')")
    @Operation(summary = "Obtener inscripciones de un grupo")
    public ResponseEntity<List<InscripcionResponseDTO>> obtenerInscripciones(@PathVariable Long idGrupo) {
        return ResponseEntity.ok(grupoService.obtenerInscripcionesConEstudiantes(idGrupo));
    }

    @GetMapping("/{idGrupo}/evaluaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('DOCENTE')")
    @Operation(summary = "Obtener evaluaciones de un grupo")
    public ResponseEntity<List<EvaluacionResponseDTO>> obtenerEvaluaciones(@PathVariable Long idGrupo) {
        return ResponseEntity.ok(grupoService.obtenerEvaluacionesPorGrupo(idGrupo));
    }

    @GetMapping("/{idGrupo}/calificaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('DOCENTE')")
    @Operation(summary = "Obtener calificaciones de un grupo")
    public ResponseEntity<List<CalificacionResponseDTO>> obtenerCalificaciones(@PathVariable Long idGrupo) {
        return ResponseEntity.ok(grupoService.obtenerCalificacionesPorGrupo(idGrupo));
    }

    @GetMapping("/por-ciclo/{idCiclo}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Listar todas las secciones registradas en un ciclo determinado")
    public ResponseEntity<List<GrupoDetalleResponseDTO>> listarGruposPorCiclo(@PathVariable Long idCiclo) {
        return ResponseEntity.ok(grupoService.listarGruposPorCiclo(idCiclo));
    }

    @GetMapping("/por-ciclo/{idCiclo}/conteo-carreras")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Obtener el consolidado de secciones agrupadas por carrera en un ciclo")
    public ResponseEntity<List<ResumenCarreraOfertaDTO>> obtenerConteoPorCarrera(@PathVariable Long idCiclo) {
        return ResponseEntity.ok(grupoService.obtenerConteoPorCarrera(idCiclo));
    }

    @DeleteMapping("/{idGrupo}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Eliminar una sección aperturada en fase de planificación")
    public ResponseEntity<Void> eliminarGrupo(@PathVariable Long idGrupo) {
        grupoService.eliminarGrupo(idGrupo);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{idGrupo}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Modificar parámetros operativos de una sección",
            description = "Permite editar docente, aforo y espacio validando traslapes (solo en ciclos en PLANIFICACIÓN).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sección actualizada satisfactoriamente"),
            @ApiResponse(responseCode = "404", description = "Sección no encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflicto de horario o espacio tras la edición"),
            @ApiResponse(responseCode = "422", description = "Ciclo no está en planificación o se excedió la capacidad")
    })
    public ResponseEntity<SeccionResponseDTO> editarGrupo(
            @Parameter(description = "Identificador único de la sección", example = "1")
            @PathVariable Long idGrupo,
            @Valid @RequestBody EditarGrupoRequestDTO request) {
        log.info("REST: Modificación de sección ID [{}]", idGrupo);
        return ResponseEntity.ok(grupoService.editarGrupo(idGrupo, request));
    }
}