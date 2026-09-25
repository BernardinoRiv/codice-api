package com.codice.sra.services;

import com.codice.sra.dtos.CalificacionRegistroRequestDTO;
import com.codice.sra.dtos.CalificacionResponseDTO;
import com.codice.sra.dtos.PlantillaNotasResponseDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EstadoCalificacionRepository estadoCalificacionRepository;
    private final GrupoRepository grupoRepository;

    public CalificacionService(
            CalificacionRepository calificacionRepository,
            EvaluacionRepository evaluacionRepository,
            InscripcionRepository inscripcionRepository,
            EstadoCalificacionRepository estadoCalificacionRepository,
            GrupoRepository grupoRepository) {
        this.calificacionRepository = calificacionRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.estadoCalificacionRepository = estadoCalificacionRepository;
        this.grupoRepository = grupoRepository;
    }

    @Transactional
    public CalificacionResponseDTO registrarCalificacion(CalificacionRegistroRequestDTO request, Long idUsuarioDocente) {
        validarNotaIndividual(request.getNota());

        Evaluacion evaluacion = evaluacionRepository.findById(request.getIdEvaluacion())
                .orElseThrow(() -> new RuntimeException("La evaluación no existe."));

        Inscripcion inscripcion = inscripcionRepository.findById(request.getIdInscripcion())
                .orElseThrow(() -> new RuntimeException("La inscripción no existe."));

        validarPropiedadGrupo(evaluacion, idUsuarioDocente);
        validarVentanaTiempo(evaluacion);

        EstadoCalificacion estadoBorrador = estadoCalificacionRepository.findByEstadoCalificacion("BORRADOR")
                .orElseThrow(() -> new RuntimeException("El estado BORRADOR no está configurado."));

        LocalDateTime ahora = LocalDateTime.now();
        Optional<Calificacion> calificacionExistente = calificacionRepository
                .findByInscripcionIdInscripcionAndEvaluacionIdEvaluacion(request.getIdInscripcion(), request.getIdEvaluacion());

        Calificacion calificacion;
        if (calificacionExistente.isPresent()) {
            calificacion = calificacionExistente.get();
            calificacion.setNota(request.getNota());
            calificacion.setFechaModificacion(ahora);
            calificacion.setEstadoCalificacion(estadoBorrador);
        } else {
            calificacion = new Calificacion();
            calificacion.setInscripcion(inscripcion);
            calificacion.setEvaluacion(evaluacion);
            calificacion.setNota(request.getNota());
            calificacion.setEstadoCalificacion(estadoBorrador);
            calificacion.setFechaRegistro(ahora);
        }

        calificacion.setUsuario(evaluacion.getGrupo().getDocente().getUsuario());
        return mapearAResponse(calificacionRepository.save(calificacion));
    }

    @Transactional(readOnly = true)
    public PlantillaNotasResponseDTO generarPlantilla(Long idGrupo) {
        Grupo grupo = grupoRepository.findById(idGrupo).orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
        List<Inscripcion> inscripciones = inscripcionRepository.findByGrupoIdGrupo(idGrupo);
        List<Evaluacion> evaluaciones = evaluacionRepository.findByIdGrupoOrderByPeriodo(idGrupo);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Notas");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle readOnlyStyle = workbook.createCellStyle();
            readOnlyStyle.setLocked(true);

            CellStyle editableStyle = workbook.createCellStyle();
            editableStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0"));
            editableStyle.setLocked(false); // Permite edición en celdas de notas

            Row headerRow = sheet.createRow(0);
            String[] headers = {"CARNET", "ESTUDIANTE"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            for (int i = 0; i < evaluaciones.size(); i++) {
                Cell cell = headerRow.createCell(i + 2);
                cell.setCellValue(evaluaciones.get(i).getTipoEvaluacion().getTipoEvaluacion().substring(0, 3).toUpperCase() + " " + evaluaciones.get(i).getNumeroEvaluacion());
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Inscripcion inscripcion : inscripciones) {
                Row row = sheet.createRow(rowNum);

                Cell cellCarnet = row.createCell(0);
                cellCarnet.setCellValue(inscripcion.getMatricula().getEstudiante().getCarnet());
                cellCarnet.setCellStyle(readOnlyStyle);

                Cell cellNombre = row.createCell(1);
                cellNombre.setCellValue(inscripcion.getMatricula().getEstudiante().getPersona().getNombres() + " " + inscripcion.getMatricula().getEstudiante().getPersona().getApellidos());
                cellNombre.setCellStyle(readOnlyStyle);

                for (int i = 0; i < evaluaciones.size(); i++) {
                    Cell cell = row.createCell(i + 2);
                    cell.setCellStyle(editableStyle);
                    Optional<Calificacion> califOpt = calificacionRepository.findByInscripcionIdInscripcionAndEvaluacionIdEvaluacion(inscripcion.getIdInscripcion(), evaluaciones.get(i).getIdEvaluacion());
                    if (califOpt.isPresent() && califOpt.get().getNota() != null) {
                        cell.setCellValue(califOpt.get().getNota().doubleValue());
                    }
                }
                rowNum++;
            }

            DataValidationHelper validationHelper = sheet.getDataValidationHelper();
            DataValidationConstraint constraint = validationHelper.createDecimalConstraint(DataValidationConstraint.OperatorType.BETWEEN, "0", "10");
            CellRangeAddressList addressList = new CellRangeAddressList(1, 100, 2, 2 + evaluaciones.size() - 1);
            DataValidation validation = validationHelper.createValidation(constraint, addressList);
            validation.setShowErrorBox(true);
            sheet.addValidationData(validation);

            sheet.protectSheet("notas2026");
            sheet.setColumnWidth(0, 15 * 256);
            sheet.setColumnWidth(1, 40 * 256);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            return new PlantillaNotasResponseDTO(
                    "Plantilla_Notas_" + grupo.getCodigoGrupo() + ".xlsx",
                    outputStream.toByteArray(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al generar la plantilla: " + e.getMessage());
        }
    }

    @Transactional
    public List<CalificacionResponseDTO> cargarNotasDesdeExcel(MultipartFile archivo, Long idGrupo, Long idUsuarioDocente) {
        List<CalificacionResponseDTO> respuestas = new ArrayList<>();
        List<Evaluacion> todasEvaluaciones = evaluacionRepository.findByIdGrupoOrderByPeriodo(idGrupo);
        LocalDateTime ahora = LocalDateTime.now();

        // Filtrar solo evaluaciones activas
        List<Evaluacion> evaluacionesActivas = todasEvaluaciones.stream()
                .filter(ev -> {
                    LocalDateTime inicio = ev.getFechaInicio();
                    LocalDateTime fin = ev.getFechaFin();
                    return !ahora.isBefore(inicio) && !ahora.isAfter(fin);
                })
                .collect(Collectors.toList());

        if (evaluacionesActivas.isEmpty()) {
            throw new RuntimeException("No hay evaluaciones activas en este momento para cargar notas.");
        }

        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Validar estructura del archivo
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("El archivo está vacío o no tiene encabezados.");
            }

            String colA = getCellValueAsString(headerRow.getCell(0));
            String colB = getCellValueAsString(headerRow.getCell(1));

            if (colA == null || !colA.toUpperCase().contains("CARNET") ||
                    colB == null || !colB.toUpperCase().contains("ESTUDIANTE")) {
                throw new RuntimeException("Estructura inválida: Las columnas A y B deben ser 'CARNET' y 'ESTUDIANTE'. Descarga la plantilla oficial.");
            }

            // Crear mapa de evaluaciones activas a sus columnas
            Map<String, Integer> mapaEvaluacionesActivas = new HashMap<>();
            for (int i = 2; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String encabezado = cell.toString().toUpperCase().trim();
                    String[] partes = encabezado.split(" ");
                    if (partes.length >= 2) {
                        String tipo = partes[0];
                        String numero = partes[1];

                        if (tipo.equals("LAB") || tipo.equals("LABORATORIO")) {
                            mapaEvaluacionesActivas.put("LABORATORIO_" + numero, i);
                        } else if (tipo.equals("PAR") || tipo.equals("PARCIAL")) {
                            mapaEvaluacionesActivas.put("PARCIAL_" + numero, i);
                        }
                    }
                }
            }

            // Validar que existan columnas para las evaluaciones activas
            for (Evaluacion ev : evaluacionesActivas) {
                String key = ev.getTipoEvaluacion().getTipoEvaluacion() + "_" + ev.getNumeroEvaluacion();
                if (!mapaEvaluacionesActivas.containsKey(key)) {
                    throw new RuntimeException("No se encontró la columna para " + ev.getTipoEvaluacion().getTipoEvaluacion() +
                            " " + ev.getNumeroEvaluacion() + " en el archivo Excel.");
                }
            }

            // Procesar filas
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                final int filaActual = i + 1;
                String carnet = getCellValueAsString(row.getCell(0));
                if (carnet == null || carnet.trim().isEmpty()) continue;

                Inscripcion inscripcion = inscripcionRepository.findByCarnetAndGrupo(carnet, idGrupo)
                        .orElseThrow(() -> new RuntimeException("Fila " + filaActual + ": Estudiante con carnet " + carnet + " no encontrado."));

                // Procesar SOLO las evaluaciones activas
                for (Evaluacion evaluacion : evaluacionesActivas) {
                    String key = evaluacion.getTipoEvaluacion().getTipoEvaluacion() + "_" + evaluacion.getNumeroEvaluacion();
                    Integer indiceColumna = mapaEvaluacionesActivas.get(key);

                    if (indiceColumna == null) continue;

                    Cell cell = row.getCell(indiceColumna);
                    if (cell == null || cell.getCellType() == CellType.BLANK) continue;

                    if (cell.getCellType() == CellType.NUMERIC) {
                        BigDecimal nota = BigDecimal.valueOf(cell.getNumericCellValue()).setScale(1, RoundingMode.HALF_DOWN);

                        if (nota.stripTrailingZeros().scale() > 1) {
                            throw new RuntimeException("Fila " + filaActual + ": La nota tiene más de 1 decimal.");
                        }

                        CalificacionRegistroRequestDTO request = new CalificacionRegistroRequestDTO();
                        request.setIdInscripcion(inscripcion.getIdInscripcion());
                        request.setIdEvaluacion(evaluacion.getIdEvaluacion());
                        request.setNota(nota);

                        respuestas.add(registrarCalificacion(request, idUsuarioDocente));
                    }
                }
            }
            return respuestas;

        } catch (Exception e) {
            throw new RuntimeException("Error al procesar el archivo Excel: " + e.getMessage());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return null;
    }

    private void validarNotaIndividual(BigDecimal nota) {
        if (nota == null) throw new IllegalArgumentException("La nota no puede ser nula.");
        if (nota.compareTo(BigDecimal.ZERO) < 0 || nota.compareTo(new BigDecimal("10.0")) > 0) {
            throw new IllegalArgumentException("La nota debe estar en el rango de 0.0 a 10.0.");
        }
        if (nota.stripTrailingZeros().scale() > 1) {
            throw new IllegalArgumentException("La nota solo puede tener 1 decimal.");
        }
    }

    private void validarPropiedadGrupo(Evaluacion evaluacion, Long idUsuarioDocente) {
        Long idUsuarioGrupo = evaluacion.getGrupo().getDocente().getUsuario().getIdUsuario();
        if (!idUsuarioGrupo.equals(idUsuarioDocente)) {
            throw new RuntimeException("No tiene autorización para calificar este grupo.");
        }
    }

    private void validarVentanaTiempo(Evaluacion evaluacion) {
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(evaluacion.getFechaInicio())) throw new RuntimeException("El período de calificación aún no ha iniciado.");
        if (ahora.isAfter(evaluacion.getFechaFin())) throw new RuntimeException("El período de calificación ha finalizado.");
    }

    private CalificacionResponseDTO mapearAResponse(Calificacion calificacion) {
        CalificacionResponseDTO response = new CalificacionResponseDTO();
        response.setIdCalificacion(calificacion.getIdCalificacion());
        response.setIdInscripcion(calificacion.getInscripcion().getIdInscripcion());
        response.setIdEvaluacion(calificacion.getEvaluacion().getIdEvaluacion());
        response.setNota(calificacion.getNota());
        response.setEstadoCalificacion(calificacion.getEstadoCalificacion().getEstadoCalificacion());
        response.setFechaRegistro(calificacion.getFechaRegistro());
        response.setFechaPublicacion(calificacion.getFechaPublicacion());
        response.setFechaModificacion(calificacion.getFechaModificacion());
        return response;
    }
}