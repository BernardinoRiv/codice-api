
package com.codice.sra.services;

import com.codice.sra.dtos.*;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfAcademicoService {

    @Value("${app.pdf.logo-path:uploads/pdf/uma-logo.jpeg}")
    private String logoPath;

    // =====================================================
    // IDENTIDAD VISUAL UMA
    // =====================================================

    private static final Color UMA_RED = new Color(200, 16, 46);
    private static final Color DARK = new Color(38, 38, 38);
    private static final Color GRAY = new Color(100, 100, 100);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color BORDER = new Color(220, 220, 220);
    private static final Color WHITE = Color.WHITE;
    private static final Color GREEN = new Color(39, 125, 75);

    private static final Font TITULO_FONT =
            font(FontFactory.HELVETICA_BOLD, 17, UMA_RED);

    private static final Font SUBTITULO_FONT =
            font(FontFactory.HELVETICA_BOLD, 10, DARK);

    private static final Font TEXTO_NORMAL =
            font(FontFactory.HELVETICA, 8, DARK);

    private static final Font TEXTO_BOLD =
            font(FontFactory.HELVETICA_BOLD, 8, DARK);

    private static final Font TEXTO_PEQUENO =
            font(FontFactory.HELVETICA, 7, GRAY);

    private static final Font HEADER_TABLA =
            font(FontFactory.HELVETICA_BOLD, 7, WHITE);

    private static final DateTimeFormatter FECHA_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =====================================================
    // 1. BOLETA DE CALIFICACIONES
    // =====================================================

    public byte[] generarBoletaNotasPDF(
            NotasEstudianteResponseDTO datosCiclo,
            HistorialAcademicoDTO historial,
            String carnet,
            String nombre
    ) {

        if (datosCiclo == null) {
            throw new IllegalArgumentException(
                    "No se recibieron los datos del ciclo."
            );
        }

        if (historial == null) {
            throw new IllegalArgumentException(
                    "No se recibió el historial académico."
            );
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Horizontal para que las diez columnas sean legibles.
            Document document = new Document(
                    PageSize.LETTER.rotate(),
                    35,
                    35,
                    35,
                    48
            );

            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PiePaginaEvent());

            document.open();

            // Encabezado institucional
            agregarEncabezado(
                    document,
                    "BOLETA DE CALIFICACIONES",
                    "REPORTE DE RENDIMIENTO ACADÉMICO"
            );

            // Información del estudiante
            agregarTituloSeccion(
                    document,
                    "INFORMACIÓN DEL ESTUDIANTE"
            );

            PdfPTable estudiante = new PdfPTable(4);
            estudiante.setWidthPercentage(100);
            estudiante.setWidths(new float[]{1f, 2.8f, 1f, 2.5f});
            estudiante.setSpacingAfter(13f);

            agregarDato(
                    estudiante,
                    "CARNET",
                    seguro(carnet)
            );

            agregarDato(
                    estudiante,
                    "CICLO",
                    seguro(datosCiclo.getCiclo())
            );

            agregarDato(
                    estudiante,
                    "ESTUDIANTE",
                    seguro(nombre)
            );

            agregarDato(
                    estudiante,
                    "CUM ACUMULADO",
                    formatoNumero(historial.getCumAcumulado(), 1)
            );

            agregarDato(
                    estudiante,
                    "CARRERA",
                    seguro(historial.getCarrera())
            );

            agregarDato(
                    estudiante,
                    "UV ACUMULADAS",
                    seguro(historial.getUvAcumuladas())
            );

            document.add(estudiante);

            // Tabla de calificaciones
            agregarTituloSeccion(
                    document,
                    "DETALLE DE CALIFICACIONES"
            );

            PdfPTable notas = new PdfPTable(10);
            notas.setWidthPercentage(100);

            notas.setWidths(new float[]{
                    27f, 10f, 7f, 7f, 7f,
                    7f, 7f, 7f, 11f, 10f
            });

            notas.setHeaderRows(1);
            notas.setSplitRows(true);
            notas.setSplitLate(false);
            notas.setSpacingAfter(14f);

            String[] encabezados = {
                    "MATERIA",
                    "CÓDIGO",
                    "L1",
                    "P1",
                    "L2",
                    "P2",
                    "L3",
                    "P3",
                    "PROMEDIO",
                    "OFICIAL"
            };

            for (String encabezado : encabezados) {
                agregarEncabezadoTabla(notas, encabezado);
            }

            boolean alternar = false;

            if (datosCiclo.getMaterias() != null) {

                for (MateriaNotasDTO materia : datosCiclo.getMaterias()) {

                    if (materia == null) {
                        continue;
                    }

                    Color fondo = alternar ? LIGHT_GRAY : WHITE;

                    agregarCelda(
                            notas,
                            seguro(materia.getNombre()),
                            TEXTO_NORMAL,
                            fondo,
                            Element.ALIGN_LEFT
                    );

                    agregarCelda(
                            notas,
                            seguro(materia.getCodigo()),
                            TEXTO_NORMAL,
                            fondo,
                            Element.ALIGN_CENTER
                    );

                    // Evita errores cuando una materia no tiene notas.
                    var calificaciones = materia.getNotas();

                    agregarCelda(
                            notas,
                            calificaciones != null
                                    ? formatoNumero(calificaciones.getLab1(), 2)
                                    : "-",
                            TEXTO_NORMAL,
                            fondo,
                            Element.ALIGN_CENTER
                    );

                    agregarCelda(
                            notas,
                            calificaciones != null
                                    ? formatoNumero(calificaciones.getPar1(), 2)
                                    : "-",
                            TEXTO_NORMAL,
                            fondo,
                            Element.ALIGN_CENTER
                    );

                    agregarCelda(
                            notas,
                            calificaciones != null
                                    ? formatoNumero(calificaciones.getLab2(), 2)
                                    : "-",
                            TEXTO_NORMAL,
                            fondo,
                            Element.ALIGN_CENTER
                    );

                    agregarCelda(
                            notas,
                            calificaciones != null
                                    ? formatoNumero(calificaciones.getPar2(), 2)
                                    : "-",
                            TEXTO_NORMAL,
                            fondo,
                            Element.ALIGN_CENTER
                    );

                    agregarCelda(
                            notas,
                            calificaciones != null
                                    ? formatoNumero(calificaciones.getLab3(), 2)
                                    : "-",
                            TEXTO_NORMAL,
                            fondo,
                            Element.ALIGN_CENTER
                    );

                    agregarCelda(
                            notas,
                            calificaciones != null
                                    ? formatoNumero(calificaciones.getPar3(), 2)
                                    : "-",
                            TEXTO_NORMAL,
                            fondo,
                            Element.ALIGN_CENTER
                    );

                    agregarCelda(
                            notas,
                            formatoNumero(
                                    materia.getPromedioSinRedondear(),
                                    3
                            ),
                            TEXTO_NORMAL,
                            fondo,
                            Element.ALIGN_CENTER
                    );

                    Font oficialFont = font(
                            FontFactory.HELVETICA_BOLD,
                            8,
                            UMA_RED
                    );

                    agregarCelda(
                            notas,
                            formatoNumero(
                                    materia.getPromedioOficial(),
                                    1
                            ),
                            oficialFont,
                            fondo,
                            Element.ALIGN_CENTER
                    );

                    alternar = !alternar;
                }
            }

            document.add(notas);

            // Horarios
            if (datosCiclo.getMaterias() != null
                    && !datosCiclo.getMaterias().isEmpty()) {

                agregarTituloSeccion(
                        document,
                        "HORARIO DE CLASES"
                );

                PdfPTable horarios = new PdfPTable(7);
                horarios.setWidthPercentage(100);

                horarios.setWidths(new float[]{
                        2.2f, 1f, 1f, 1f, 1f, 1f, 1f
                });

                horarios.setHeaderRows(1);
                horarios.setSplitRows(true);
                horarios.setSplitLate(false);

                String[] dias = {
                        "MATERIA",
                        "LUNES",
                        "MARTES",
                        "MIÉRCOLES",
                        "JUEVES",
                        "VIERNES",
                        "SÁBADO"
                };

                for (String dia : dias) {
                    agregarEncabezadoTabla(horarios, dia);
                }

                alternar = false;

                for (MateriaNotasDTO materia : datosCiclo.getMaterias()) {

                    if (materia == null) {
                        continue;
                    }

                    Color fondo = alternar ? LIGHT_GRAY : WHITE;

                    agregarCelda(
                            horarios,
                            seguro(materia.getNombre()),
                            TEXTO_NORMAL,
                            fondo,
                            Element.ALIGN_LEFT
                    );

                    String[] horas = {
                            "-", "-", "-", "-", "-", "-"
                    };

                    if (materia.getHorarios() != null) {

                        for (HorarioDTO horario : materia.getHorarios()) {

                            if (horario == null
                                    || horario.getDia() == null) {
                                continue;
                            }

                            String dia = horario.getDia()
                                    .trim()
                                    .toUpperCase(Locale.ROOT);

                            String textoHora =
                                    seguro(horario.getHoraInicio())
                                            + " - "
                                            + seguro(horario.getHoraFin());

                            switch (dia) {
                                case "LUNES":
                                    horas[0] = textoHora;
                                    break;

                                case "MARTES":
                                    horas[1] = textoHora;
                                    break;

                                case "MIÉRCOLES":
                                case "MIERCOLES":
                                    horas[2] = textoHora;
                                    break;

                                case "JUEVES":
                                    horas[3] = textoHora;
                                    break;

                                case "VIERNES":
                                    horas[4] = textoHora;
                                    break;

                                case "SÁBADO":
                                case "SABADO":
                                    horas[5] = textoHora;
                                    break;
                            }
                        }
                    }

                    for (String hora : horas) {
                        agregarCelda(
                                horarios,
                                hora,
                                TEXTO_PEQUENO,
                                fondo,
                                Element.ALIGN_CENTER
                        );
                    }

                    alternar = !alternar;
                }

                document.add(horarios);
            }

            document.close();
            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error al generar la boleta de calificaciones PDF.",
                    e
            );
        }
    }

    // =====================================================
    // 2. HISTORIAL ACADÉMICO
    // =====================================================

    public byte[] generarHistorialAcademicoPDF(
            HistorialAcademicoDTO historial,
            String carnet,
            String nombre,
            String filtroCiclo
    ) {

        if (historial == null) {
            throw new IllegalArgumentException(
                    "No se recibió el historial académico."
            );
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(
                    PageSize.LETTER,
                    38,
                    38,
                    35,
                    48
            );

            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PiePaginaEvent());

            document.open();

            agregarEncabezado(
                    document,
                    "HISTORIAL ACADÉMICO",
                    "REGISTRO DEL RENDIMIENTO ACADÉMICO"
            );

            // Datos generales
            agregarTituloSeccion(
                    document,
                    "INFORMACIÓN DEL ESTUDIANTE"
            );

            PdfPTable estudiante = new PdfPTable(4);
            estudiante.setWidthPercentage(100);
            estudiante.setWidths(new float[]{1f, 2.4f, 1f, 2.4f});
            estudiante.setSpacingAfter(15f);

            agregarDato(
                    estudiante,
                    "CARNET",
                    seguro(carnet)
            );

            agregarDato(
                    estudiante,
                    "CUM GLOBAL",
                    formatoNumero(historial.getCumAcumulado(), 1)
            );

            agregarDato(
                    estudiante,
                    "ESTUDIANTE",
                    seguro(nombre)
            );

            agregarDato(
                    estudiante,
                    "UV ACUMULADAS",
                    seguro(historial.getUvAcumuladas())
            );

            agregarDato(
                    estudiante,
                    "CARRERA",
                    seguro(historial.getCarrera())
            );

            agregarDato(
                    estudiante,
                    "FILTRO",
                    filtroCiclo == null || filtroCiclo.isBlank()
                            ? "TODOS"
                            : filtroCiclo
            );

            document.add(estudiante);

            agregarTituloSeccion(
                    document,
                    "MATERIAS POR CICLO"
            );

            boolean hayResultados = false;

            if (historial.getMateriasPorCiclo() != null) {

                for (MateriaCicloDTO ciclo
                        : historial.getMateriasPorCiclo()) {

                    if (ciclo == null) {
                        continue;
                    }

                    String codigoCiclo = seguro(ciclo.getCodigoCiclo());

                    boolean todos = filtroCiclo == null
                            || filtroCiclo.isBlank()
                            || filtroCiclo.equalsIgnoreCase("TODOS");

                    if (!todos
                            && !codigoCiclo.equalsIgnoreCase(filtroCiclo)) {
                        continue;
                    }

                    hayResultados = true;

                    // Encabezado del ciclo
                    PdfPTable cycleHeader = new PdfPTable(2);
                    cycleHeader.setWidthPercentage(100);
                    cycleHeader.setWidths(new float[]{1f, 1f});
                    cycleHeader.setSpacingBefore(8f);
                    cycleHeader.setSpacingAfter(5f);

                    PdfPCell cycleName = new PdfPCell(
                            new Phrase(
                                    "CICLO: " + codigoCiclo,
                                    font(
                                            FontFactory.HELVETICA_BOLD,
                                            9,
                                            WHITE
                                    )
                            )
                    );

                    cycleName.setBackgroundColor(UMA_RED);
                    cycleName.setBorder(Rectangle.NO_BORDER);
                    cycleName.setPadding(8f);

                    PdfPCell cycleStatus = new PdfPCell(
                            new Phrase(
                                    "ESTADO: " + seguro(ciclo.getEstadoCiclo()),
                                    font(
                                            FontFactory.HELVETICA_BOLD,
                                            8,
                                            WHITE
                                    )
                            )
                    );

                    cycleStatus.setBackgroundColor(UMA_RED);
                    cycleStatus.setBorder(Rectangle.NO_BORDER);
                    cycleStatus.setPadding(8f);
                    cycleStatus.setHorizontalAlignment(
                            Element.ALIGN_RIGHT
                    );

                    cycleHeader.addCell(cycleName);
                    cycleHeader.addCell(cycleStatus);

                    document.add(cycleHeader);

                    PdfPTable materias = new PdfPTable(5);
                    materias.setWidthPercentage(100);
                    materias.setWidths(new float[]{
                            1.1f, 3.5f, 0.7f, 1.1f, 1.3f
                    });

                    materias.setHeaderRows(1);
                    materias.setSplitRows(true);
                    materias.setSplitLate(false);
                    materias.setSpacingAfter(12f);

                    String[] encabezados = {
                            "CÓDIGO",
                            "MATERIA",
                            "UV",
                            "NOTA FINAL",
                            "ESTADO"
                    };

                    for (String encabezado : encabezados) {
                        agregarEncabezadoTabla(materias, encabezado);
                    }

                    boolean alternar = false;

                    if (ciclo.getMaterias() != null) {

                        for (MateriaHistorialDTO materia
                                : ciclo.getMaterias()) {

                            if (materia == null) {
                                continue;
                            }

                            Color fondo = alternar
                                    ? LIGHT_GRAY
                                    : WHITE;

                            agregarCelda(
                                    materias,
                                    seguro(materia.getCodigoMateria()),
                                    TEXTO_NORMAL,
                                    fondo,
                                    Element.ALIGN_CENTER
                            );

                            agregarCelda(
                                    materias,
                                    seguro(materia.getNombreMateria()),
                                    TEXTO_NORMAL,
                                    fondo,
                                    Element.ALIGN_LEFT
                            );

                            agregarCelda(
                                    materias,
                                    seguro(materia.getUv()),
                                    TEXTO_NORMAL,
                                    fondo,
                                    Element.ALIGN_CENTER
                            );

                            agregarCelda(
                                    materias,
                                    formatoNumero(materia.getNotaFinal(), 1),
                                    TEXTO_NORMAL,
                                    fondo,
                                    Element.ALIGN_CENTER
                            );

                            agregarCeldaEstado(
                                    materias,
                                    seguro(materia.getEstado()),
                                    fondo
                            );

                            alternar = !alternar;
                        }
                    }

                    document.add(materias);
                }
            }

            if (!hayResultados) {

                PdfPTable emptyTable = new PdfPTable(1);
                emptyTable.setWidthPercentage(100);

                PdfPCell emptyCell = new PdfPCell(
                        new Phrase(
                                "No se encontraron materias para el ciclo seleccionado.",
                                TEXTO_NORMAL
                        )
                );

                emptyCell.setBackgroundColor(LIGHT_GRAY);
                emptyCell.setBorderColor(BORDER);
                emptyCell.setPadding(15f);
                emptyCell.setHorizontalAlignment(
                        Element.ALIGN_CENTER
                );

                emptyTable.addCell(emptyCell);
                document.add(emptyTable);
            }

            document.close();
            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error al generar el historial académico PDF.",
                    e
            );
        }
    }

    // =====================================================
    // 3. ENCABEZADO INSTITUCIONAL
    // =====================================================

    private void agregarEncabezado(
            Document document,
            String titulo,
            String subtitulo
    ) throws Exception {

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1f, 2.5f});
        header.setSpacingAfter(10f);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(5f);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        try {

            File logoFile = new File(logoPath);

            if (logoFile.exists() && logoFile.isFile()) {

                Image logo = Image.getInstance(
                        logoFile.getAbsolutePath()
                );

                logo.scaleToFit(105, 65);
                logo.setAlignment(Element.ALIGN_CENTER);

                logoCell.addElement(logo);
            }

        } catch (Exception e) {
            // Se conserva el encabezado textual si falla el logo.
        }

        header.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph institution = new Paragraph(
                "UNIVERSIDAD MODULAR ABIERTA",
                font(FontFactory.HELVETICA_BOLD, 11, DARK)
        );

        institution.setAlignment(Element.ALIGN_RIGHT);

        Paragraph regional = new Paragraph(
                "CENTRO REGIONAL DE SANTA ANA",
                font(FontFactory.HELVETICA_BOLD, 8, GRAY)
        );

        regional.setAlignment(Element.ALIGN_RIGHT);

        Paragraph reportTitle = new Paragraph(titulo, TITULO_FONT);
        reportTitle.setAlignment(Element.ALIGN_RIGHT);
        reportTitle.setSpacingBefore(7f);

        Paragraph reportSubtitle = new Paragraph(
                subtitulo,
                TEXTO_PEQUENO
        );

        reportSubtitle.setAlignment(Element.ALIGN_RIGHT);

        titleCell.addElement(institution);
        titleCell.addElement(regional);
        titleCell.addElement(reportTitle);
        titleCell.addElement(reportSubtitle);

        header.addCell(titleCell);

        document.add(header);

        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingAfter(12f);

        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(UMA_RED);
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setFixedHeight(3f);

        line.addCell(lineCell);

        document.add(line);
    }

    // =====================================================
    // 4. COMPONENTES DE TABLAS
    // =====================================================

    private void agregarTituloSeccion(
            Document document,
            String titulo
    ) throws DocumentException {

        Paragraph paragraph = new Paragraph(
                titulo,
                SUBTITULO_FONT
        );

        paragraph.setSpacingBefore(5f);
        paragraph.setSpacingAfter(7f);

        document.add(paragraph);
    }

    private void agregarDato(
            PdfPTable table,
            String label,
            String value
    ) {

        PdfPCell labelCell = new PdfPCell(
                new Phrase(label, TEXTO_BOLD)
        );

        labelCell.setBackgroundColor(LIGHT_GRAY);
        labelCell.setBorderColor(BORDER);
        labelCell.setPadding(7f);

        PdfPCell valueCell = new PdfPCell(
                new Phrase(value, TEXTO_NORMAL)
        );

        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(7f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void agregarEncabezadoTabla(
            PdfPTable table,
            String texto
    ) {

        PdfPCell cell = new PdfPCell(
                new Phrase(texto, HEADER_TABLA)
        );

        cell.setBackgroundColor(UMA_RED);
        cell.setBorderColor(WHITE);
        cell.setPadding(7f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private void agregarCelda(
            PdfPTable table,
            String texto,
            Font font,
            Color background,
            int alignment
    ) {

        PdfPCell cell = new PdfPCell(
                new Phrase(texto, font)
        );

        cell.setBackgroundColor(background);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBorderColor(BORDER);

        table.addCell(cell);
    }

    private void agregarCeldaEstado(
            PdfPTable table,
            String estado,
            Color background
    ) {

        String normalized = estado.toUpperCase(Locale.ROOT);

        Color textColor = normalized.contains("APROB")
                ? GREEN
                : DARK;

        Font stateFont = font(
                FontFactory.HELVETICA_BOLD,
                7,
                textColor
        );

        agregarCelda(
                table,
                estado,
                stateFont,
                background,
                Element.ALIGN_CENTER
        );
    }

    // =====================================================
    // 5. FUNCIONES DE FORMATO Y SEGURIDAD
    // =====================================================

    private static Font font(
            String family,
            int size,
            Color color
    ) {

        return FontFactory.getFont(
                family,
                size,
                color
        );
    }

    private String seguro(Object value) {

        if (value == null) {
            return "No disponible";
        }

        String text = String.valueOf(value).trim();

        if (text.isEmpty() || text.equalsIgnoreCase("N/A")) {
            return "No disponible";
        }

        return text;
    }

    private String formatoNumero(Object value, int decimals) {

        if (value == null) {
            return "-";
        }

        try {

            double number = Double.parseDouble(
                    String.valueOf(value)
            );

            return String.format(
                    Locale.US,
                    "%." + decimals + "f",
                    number
            );

        } catch (NumberFormatException e) {
            return "-";
        }
    }

    // =====================================================
    // 6. PIE DE PÁGINA
    // =====================================================

    private static class PiePaginaEvent extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {

            PdfContentByte canvas = writer.getDirectContent();

            float left = document.left();
            float right = document.right();
            float y = document.bottom() - 22f;

            canvas.saveState();

            canvas.setColorStroke(BORDER);
            canvas.setLineWidth(0.7f);

            canvas.moveTo(left, y + 12f);
            canvas.lineTo(right, y + 12f);
            canvas.stroke();

            Font footerFont = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    7,
                    GRAY
            );

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_LEFT,
                    new Phrase(
                            "Universidad Modular Abierta | Centro Regional de Santa Ana",
                            footerFont
                    ),
                    left,
                    y,
                    0
            );

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_RIGHT,
                    new Phrase(
                            "Página " + writer.getPageNumber()
                                    + " | Generado: "
                                    + LocalDateTime.now().format(FECHA_FORMAT),
                            footerFont
                    ),
                    right,
                    y,
                    0
            );

            canvas.restoreState();
        }
    }
}
