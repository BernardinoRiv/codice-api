
package com.codice.sra.services;

import com.codice.sra.dtos.ClaseInscritaDTO;
import com.codice.sra.dtos.HorarioDTO;
import com.codice.sra.dtos.MateriaPensumDTO;
import com.codice.sra.dtos.PensumEstudianteDTO;
import com.codice.sra.models.Estudiante;
import com.codice.sra.repositories.EstudianteRepository;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstudiantePdfService {

    private final EstudianteClasesService estudianteClasesService;
    private final EstudianteRepository estudianteRepository;

    @Value("${app.pdf.logo-path:uploads/pdf/uma-logo.jpeg}")
    private String logoPath;

    // ==========================================
    // IDENTIDAD VISUAL UMA
    // ==========================================

    private static final BaseColor ROJO =
            new BaseColor(200, 16, 46);

    private static final BaseColor ROJO_OSCURO =
            new BaseColor(151, 12, 35);

    private static final BaseColor GRIS_OSCURO =
            new BaseColor(48, 52, 58);

    private static final BaseColor GRIS_TEXTO =
            new BaseColor(105, 110, 118);

    private static final BaseColor GRIS_CLARO =
            new BaseColor(245, 246, 248);

    private static final BaseColor BLANCO =
            BaseColor.WHITE;

    private static final BaseColor VERDE =
            new BaseColor(39, 143, 91);

    private static final BaseColor AZUL =
            new BaseColor(48, 112, 180);

    private static final BaseColor AMARILLO =
            new BaseColor(217, 153, 35);

    private static final BaseColor BORDE =
            new BaseColor(225, 228, 232);

    // ==========================================
    // TIPOGRAFÍAS
    // ==========================================

    private Font fuenteTitulo() {
        return FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                19,
                GRIS_OSCURO
        );
    }

    private Font fuenteSubtitulo() {
        return FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                11,
                ROJO
        );
    }

    private Font fuenteNormal() {
        return FontFactory.getFont(
                FontFactory.HELVETICA,
                8.5f,
                GRIS_OSCURO
        );
    }

    private Font fuentePequena() {
        return FontFactory.getFont(
                FontFactory.HELVETICA,
                7.5f,
                GRIS_TEXTO
        );
    }

    private Font fuenteNegrita() {
        return FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                8.5f,
                GRIS_OSCURO
        );
    }

    private Font fuenteBlanca() {
        return FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                8.5f,
                BLANCO
        );
    }

    // ==========================================
    // GENERACIÓN PRINCIPAL
    // ==========================================

    public byte[] generarDocumentoAcademico(String tipo) throws Exception {

        if (tipo == null ||
                (!tipo.equalsIgnoreCase("clases")
                        && !tipo.equalsIgnoreCase("pensum"))) {
            throw new IllegalArgumentException(
                    "Tipo de documento no válido. Use clases o pensum."
            );
        }

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException(
                    "No existe una sesión autenticada."
            );
        }

        String identificador = auth.getName();

        Estudiante estudiante;

        try {
            Long idUsuario = Long.parseLong(identificador);

            estudiante = estudianteRepository
                    .findByUsuario_IdUsuario(idUsuario)
                    .orElseThrow(() -> new RuntimeException(
                            "No se encontró el estudiante asociado al usuario."
                    ));

        } catch (NumberFormatException e) {

            estudiante = estudianteRepository
                    .findByCarnet(identificador)
                    .orElseGet(() ->
                            estudianteRepository
                                    .findByUsuario_CorreoInstitucional(identificador)
                                    .orElseThrow(() -> new RuntimeException(
                                            "No se encontró el estudiante."
                                    ))
                    );
        }

        Long idEstudiante = estudiante.getIdEstudiante();

        String nombre = estudiante.getPersona().getNombres()
                + " " + estudiante.getPersona().getApellidos();

        String carnet = estudiante.getCarnet();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(
                PageSize.A4,
                36,
                36,
                100,
                55
        );

        PdfWriter writer = PdfWriter.getInstance(document, out);

        // Encabezado y pie institucional en todas las páginas.
        writer.setPageEvent(new EventoPaginaUMA(cargarLogo()));

        document.open();

        agregarTituloDocumento(document, tipo);

        agregarInformacionEstudiante(
                document,
                nombre,
                carnet
        );

        if ("clases".equalsIgnoreCase(tipo)) {

            generarContenidoClases(
                    document,
                    idEstudiante
            );

        } else {

            generarContenidoPensum(
                    document,
                    idEstudiante
            );
        }

        document.close();

        return out.toByteArray();
    }

    // ==========================================
    // CARGA DEL LOGOTIPO
    // ==========================================

    private Image cargarLogo() {

        try {

            File archivo = new File(logoPath);

            if (!archivo.exists() || !archivo.isFile()) {
                return null;
            }

            Image logo = Image.getInstance(archivo.getAbsolutePath());

            logo.scaleToFit(65, 65);

            return logo;

        } catch (Exception e) {

            return null;
        }
    }

    // ==========================================
    // TÍTULO DEL DOCUMENTO
    // ==========================================

    private void agregarTituloDocumento(
            Document document,
            String tipo
    ) throws DocumentException {

        String titulo = tipo.equalsIgnoreCase("clases")
                ? "REPORTE DE CLASES INSCRITAS"
                : "REPORTE DE PROGRESO ACADÉMICO";

        Paragraph encabezado = new Paragraph(
                titulo,
                fuenteTitulo()
        );

        encabezado.setAlignment(Element.ALIGN_LEFT);
        encabezado.setSpacingBefore(5);
        encabezado.setSpacingAfter(5);

        document.add(encabezado);

        Paragraph descripcion = new Paragraph(
                tipo.equalsIgnoreCase("clases")
                        ? "Detalle de asignaturas correspondientes al ciclo académico activo."
                        : "Seguimiento del cumplimiento del plan de estudios.",
                fuentePequena()
        );

        descripcion.setSpacingAfter(14);

        document.add(descripcion);

        LineSeparator linea = new LineSeparator(
                1.5f,
                100,
                ROJO,
                Element.ALIGN_CENTER,
                -2
        );

        document.add(linea);
        document.add(new Paragraph(" "));
    }

    // ==========================================
    // INFORMACIÓN DEL ESTUDIANTE
    // ==========================================

    private void agregarInformacionEstudiante(
            Document document,
            String nombre,
            String carnet
    ) throws DocumentException {

        PdfPTable tabla = new PdfPTable(2);

        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{2.5f, 1.2f});
        tabla.setSpacingBefore(7);
        tabla.setSpacingAfter(15);

        PdfPCell estudiante = crearCeldaInformativa(
                "ESTUDIANTE",
                nombre
        );

        PdfPCell codigo = crearCeldaInformativa(
                "CARNET UNIVERSITARIO",
                carnet
        );

        tabla.addCell(estudiante);
        tabla.addCell(codigo);

        PdfPCell fecha = crearCeldaInformativa(
                "FECHA DE EMISIÓN",
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )
        );

        fecha.setColspan(2);
        tabla.addCell(fecha);

        document.add(tabla);
    }

    private PdfPCell crearCeldaInformativa(
            String etiqueta,
            String valor
    ) {

        PdfPCell celda = new PdfPCell();

        celda.setBackgroundColor(GRIS_CLARO);
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.7f);
        celda.setPadding(10);

        Paragraph titulo = new Paragraph(
                etiqueta,
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        7,
                        ROJO
                )
        );

        Paragraph contenido = new Paragraph(
                valor != null ? valor : "No disponible",
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        9,
                        GRIS_OSCURO
                )
        );

        contenido.setSpacingBefore(4);

        celda.addElement(titulo);
        celda.addElement(contenido);

        return celda;
    }

    // ==========================================
    // REPORTE DE CLASES INSCRITAS
    // ==========================================

    private void generarContenidoClases(
            Document document,
            Long idEstudiante
    ) throws DocumentException {

        List<ClaseInscritaDTO> clases =
                estudianteClasesService
                        .obtenerClasesCicloActivo(idEstudiante);

        if (clases == null) {
            clases = new ArrayList<>();
        }

        clases = clases.stream()
                .filter(c -> c.getEstadoMateria() == null
                        || !c.getEstadoMateria().equalsIgnoreCase("retirada"))
                .collect(Collectors.toList());

        agregarTituloSeccion(
                document,
                "Asignaturas inscritas",
                "Detalle de materias y horarios registrados."
        );

        if (clases.isEmpty()) {

            agregarMensajeVacio(
                    document,
                    "No se encontraron clases inscritas activas para el ciclo actual."
            );

            return;
        }

        PdfPTable tabla = new PdfPTable(
                new float[]{1.5f, 4f, 1.8f, 0.8f, 1.7f}
        );

        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.setSpacingBefore(7);
        tabla.setSpacingAfter(10);
        tabla.setSplitRows(false);

        agregarCeldaEncabezado(tabla, "Código");
        agregarCeldaEncabezado(tabla, "Asignatura");
        agregarCeldaEncabezado(tabla, "Grupo");
        agregarCeldaEncabezado(tabla, "UV");
        agregarCeldaEncabezado(tabla, "Estado");

        int indice = 0;

        for (ClaseInscritaDTO clase : clases) {

            BaseColor fondo = indice % 2 == 0
                    ? BLANCO
                    : GRIS_CLARO;

            tabla.addCell(crearCelda(
                    clase.getCodigoMateria(),
                    fondo,
                    Element.ALIGN_CENTER
            ));

            tabla.addCell(crearCelda(
                    clase.getNombreMateria(),
                    fondo,
                    Element.ALIGN_LEFT
            ));

            tabla.addCell(crearCelda(
                    clase.getGrupo(),
                    fondo,
                    Element.ALIGN_CENTER
            ));

            tabla.addCell(crearCelda(
                    String.valueOf(clase.getUvs()),
                    fondo,
                    Element.ALIGN_CENTER
            ));

            tabla.addCell(crearCeldaEstado(
                    clase.getEstadoMateria()
            ));

            indice++;

            // Horarios asociados a cada asignatura.
            List<HorarioDTO> horarios = clase.getHorarios();

            if (horarios != null && !horarios.isEmpty()) {

                PdfPCell celdaHorario = new PdfPCell();

                celdaHorario.setColspan(5);
                celdaHorario.setBackgroundColor(
                        new BaseColor(250, 248, 248)
                );
                celdaHorario.setBorderColor(BORDE);
                celdaHorario.setPadding(8);

                Paragraph tituloHorario = new Paragraph(
                        "HORARIOS Y MODALIDAD",
                        FontFactory.getFont(
                                FontFactory.HELVETICA_BOLD,
                                7,
                                ROJO
                        )
                );

                celdaHorario.addElement(tituloHorario);

                for (HorarioDTO horario : horarios) {

                    String texto = String.format(
                            "%s | %s - %s | Aula: %s | Modalidad: %s",
                            valorSeguro(horario.getDia()),
                            horario.getHoraInicio() != null
                                    ? horario.getHoraInicio().toString()
                                    : "No disponible",
                            horario.getHoraFin() != null
                                    ? horario.getHoraFin().toString()
                                    : "No disponible",
                            valorSeguro(horario.getAula()),
                            valorSeguro(horario.getModalidad())
                    );

                    Paragraph detalle = new Paragraph(
                            texto,
                            fuentePequena()
                    );

                    detalle.setSpacingBefore(3);

                    celdaHorario.addElement(detalle);
                }

                tabla.addCell(celdaHorario);
            }
        }

        document.add(tabla);

        agregarNotaInstitucional(
                document,
                "Este reporte refleja las asignaturas activas registradas en el ciclo académico consultado."
        );
    }

    // ==========================================
    // REPORTE DE PENSUM
    // ==========================================

    private void generarContenidoPensum(
            Document document,
            Long idEstudiante
    ) throws DocumentException {

        PensumEstudianteDTO pensum =
                estudianteClasesService.obtenerPensum(idEstudiante);

        if (pensum == null) {

            agregarMensajeVacio(
                    document,
                    "No se encontró información del pensum académico."
            );

            return;
        }

        agregarTituloSeccion(
                document,
                "Resumen del progreso académico",
                "Estado general del cumplimiento del plan de estudios."
        );

        agregarResumenPensum(document, pensum);

        agregarTituloSeccion(
                document,
                "Detalle por ciclo académico",
                "Asignaturas, unidades valorativas y prerrequisitos."
        );

        Map<Integer, List<MateriaPensumDTO>> materias =
                pensum.getMateriasPorCiclo();

        if (materias == null || materias.isEmpty()) {

            agregarMensajeVacio(
                    document,
                    "No existen materias registradas en el pensum."
            );

            return;
        }

        List<Integer> ciclos = materias.keySet()
                .stream()
                .sorted()
                .collect(Collectors.toList());

        for (Integer ciclo : ciclos) {

            List<MateriaPensumDTO> lista = materias.get(ciclo);

            if (lista == null || lista.isEmpty()) {
                continue;
            }

            Paragraph tituloCiclo = new Paragraph(
                    "CICLO ACADÉMICO " + ciclo,
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            10,
                            ROJO
                    )
            );

            tituloCiclo.setSpacingBefore(12);
            tituloCiclo.setSpacingAfter(6);
            tituloCiclo.setKeepTogether(true);

            document.add(tituloCiclo);

            PdfPTable tabla = new PdfPTable(
                    new float[]{1.4f, 3.5f, 3.3f, 0.8f, 1.5f}
            );

            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            tabla.setSpacingAfter(10);
            tabla.setSplitRows(false);

            agregarCeldaEncabezado(tabla, "Código");
            agregarCeldaEncabezado(tabla, "Asignatura");
            agregarCeldaEncabezado(tabla, "Prerrequisitos");
            agregarCeldaEncabezado(tabla, "UV");
            agregarCeldaEncabezado(tabla, "Estado");

            int indice = 0;

            for (MateriaPensumDTO materia : lista) {

                BaseColor fondo = indice % 2 == 0
                        ? BLANCO
                        : GRIS_CLARO;

                tabla.addCell(crearCelda(
                        materia.getCodigo(),
                        fondo,
                        Element.ALIGN_CENTER
                ));

                tabla.addCell(crearCelda(
                        materia.getNombre(),
                        fondo,
                        Element.ALIGN_LEFT
                ));

                String prerrequisitos =
                        materia.getPrerrequisitos() != null
                                && !materia.getPrerrequisitos().isEmpty()
                                ? String.join(", ", materia.getPrerrequisitos())
                                : "Bachillerato";

                tabla.addCell(crearCelda(
                        prerrequisitos,
                        fondo,
                        Element.ALIGN_LEFT
                ));

                tabla.addCell(crearCelda(
                        String.valueOf(materia.getUvs()),
                        fondo,
                        Element.ALIGN_CENTER
                ));

                tabla.addCell(crearCeldaEstado(
                        materia.getEstado()
                ));

                indice++;
            }

            document.add(tabla);
        }

        agregarNotaInstitucional(
                document,
                "El avance académico se calcula de acuerdo con los estados de las asignaturas registrados en el sistema."
        );
    }

    // ==========================================
    // RESUMEN VISUAL DEL PENSUM
    // ==========================================

    private void agregarResumenPensum(
            Document document,
            PensumEstudianteDTO pensum
    ) throws DocumentException {

        PdfPTable tarjetas = new PdfPTable(3);

        tarjetas.setWidthPercentage(100);
        tarjetas.setWidths(new float[]{1, 1, 1});
        tarjetas.setSpacingBefore(8);
        tarjetas.setSpacingAfter(10);

        tarjetas.addCell(crearTarjeta(
                "APROBADAS",
                String.valueOf(pensum.getMateriasAprobadas()),
                VERDE
        ));

        tarjetas.addCell(crearTarjeta(
                "CURSANDO",
                String.valueOf(pensum.getMateriasCursando()),
                AZUL
        ));

        tarjetas.addCell(crearTarjeta(
                "PENDIENTES",
                String.valueOf(pensum.getMateriasPendientes()),
                AMARILLO
        ));

        document.add(tarjetas);

        // Indicador gráfico de avance.
        double avance;

        try {
            avance = Double.parseDouble(
                    String.valueOf(pensum.getPorcentajeAvance())
            );

        } catch (Exception e) {
            avance = 0;
        }

        avance = Math.max(0, Math.min(100, avance));

        Paragraph etiqueta = new Paragraph(
                "AVANCE GENERAL DEL PENSUM: "
                        + String.format("%.1f", avance) + "%",
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        9,
                        GRIS_OSCURO
                )
        );

        etiqueta.setSpacingBefore(5);
        etiqueta.setSpacingAfter(5);

        document.add(etiqueta);

        PdfPTable barra = new PdfPTable(2);
        barra.setWidthPercentage(100);
        barra.setWidths(new float[]{
                (float) Math.max(avance, 0.01),
                (float) Math.max(100 - avance, 0.01)
        });

        PdfPCell progreso = new PdfPCell(new Phrase(" "));
        progreso.setBackgroundColor(ROJO);
        progreso.setBorder(Rectangle.NO_BORDER);
        progreso.setFixedHeight(12);

        PdfPCell restante = new PdfPCell(new Phrase(" "));
        restante.setBackgroundColor(GRIS_CLARO);
        restante.setBorder(Rectangle.NO_BORDER);
        restante.setFixedHeight(12);

        barra.addCell(progreso);
        barra.addCell(restante);

        document.add(barra);
        document.add(new Paragraph(" "));
    }

    private PdfPCell crearTarjeta(
            String titulo,
            String valor,
            BaseColor color
    ) {

        PdfPCell celda = new PdfPCell();

        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.8f);
        celda.setPadding(10);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setBackgroundColor(GRIS_CLARO);

        Paragraph etiqueta = new Paragraph(
                titulo,
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        7,
                        GRIS_TEXTO
                )
        );

        etiqueta.setAlignment(Element.ALIGN_CENTER);

        Paragraph numero = new Paragraph(
                valor,
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        19,
                        color
                )
        );

        numero.setAlignment(Element.ALIGN_CENTER);
        numero.setSpacingBefore(5);

        celda.addElement(etiqueta);
        celda.addElement(numero);

        return celda;
    }

    // ==========================================
    // COMPONENTES DE TABLAS
    // ==========================================

    private void agregarCeldaEncabezado(
            PdfPTable tabla,
            String texto
    ) {

        PdfPCell celda = new PdfPCell(
                new Phrase(texto, fuenteBlanca())
        );

        celda.setBackgroundColor(ROJO);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(8);
        celda.setBorderColor(ROJO_OSCURO);
        celda.setBorderWidth(0.5f);

        tabla.addCell(celda);
    }

    private PdfPCell crearCelda(
            String texto,
            BaseColor fondo,
            int alineacion
    ) {

        PdfPCell celda = new PdfPCell(
                new Phrase(
                        valorSeguro(texto),
                        fuenteNormal()
                )
        );

        celda.setBackgroundColor(fondo);
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(7);
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.5f);

        return celda;
    }

    // ==========================================
    // ESTADOS ACADÉMICOS
    // ==========================================

    private PdfPCell crearCeldaEstado(String estado) {

        String texto = valorSeguro(estado);

        String normalizado = texto.toLowerCase()
                .trim();

        BaseColor fondo;
        BaseColor color;

        if (normalizado.contains("aprobada")
                || normalizado.contains("aprobado")) {

            fondo = new BaseColor(226, 243, 233);
            color = VERDE;

        } else if (normalizado.contains("cursando")
                || normalizado.contains("inscrita")
                || normalizado.contains("inscrito")) {

            fondo = new BaseColor(228, 239, 251);
            color = AZUL;

        } else if (normalizado.contains("pendiente")) {

            fondo = new BaseColor(251, 241, 218);
            color = new BaseColor(155, 108, 20);

        } else if (normalizado.contains("retirada")
                || normalizado.contains("reprobada")
                || normalizado.contains("reprobado")) {

            fondo = new BaseColor(250, 229, 231);
            color = ROJO;

        } else {

            fondo = GRIS_CLARO;
            color = GRIS_TEXTO;
        }

        PdfPCell celda = new PdfPCell(
                new Phrase(
                        texto.toUpperCase(),
                        FontFactory.getFont(
                                FontFactory.HELVETICA_BOLD,
                                7,
                                color
                        )
                )
        );

        celda.setBackgroundColor(fondo);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(7);
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.5f);

        return celda;
    }

    // ==========================================
    // TÍTULOS DE SECCIÓN
    // ==========================================

    private void agregarTituloSeccion(
            Document document,
            String titulo,
            String descripcion
    ) throws DocumentException {

        Paragraph encabezado = new Paragraph(
                titulo,
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        12,
                        GRIS_OSCURO
                )
        );

        encabezado.setSpacingBefore(10);
        encabezado.setSpacingAfter(3);
        encabezado.setKeepTogether(true);

        Paragraph detalle = new Paragraph(
                descripcion,
                fuentePequena()
        );

        detalle.setSpacingAfter(5);
        detalle.setKeepTogether(true);

        document.add(encabezado);
        document.add(detalle);
    }

    // ==========================================
    // MENSAJES Y NOTAS
    // ==========================================

    private void agregarMensajeVacio(
            Document document,
            String mensaje
    ) throws DocumentException {

        PdfPCell celda = new PdfPCell(
                new Phrase(mensaje, fuenteNormal())
        );

        celda.setBackgroundColor(GRIS_CLARO);
        celda.setBorderColor(BORDE);
        celda.setPadding(15);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.addCell(celda);

        document.add(tabla);
    }

    private void agregarNotaInstitucional(
            Document document,
            String texto
    ) throws DocumentException {

        Paragraph nota = new Paragraph(
                texto,
                FontFactory.getFont(
                        FontFactory.HELVETICA,
                        7.5f,
                        GRIS_TEXTO
                )
        );

        nota.setSpacingBefore(12);
        nota.setAlignment(Element.ALIGN_JUSTIFIED);

        document.add(nota);
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.trim().isEmpty()
                ? "No disponible"
                : valor.trim();
    }

    // ==========================================
    // ENCABEZADO Y PIE DE PÁGINA
    // ==========================================

    private class EventoPaginaUMA extends PdfPageEventHelper {

        private final Image logo;

        private final Font fuenteInstitucional =
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        11,
                        GRIS_OSCURO
                );

        private final Font fuenteDetalle =
                FontFactory.getFont(
                        FontFactory.HELVETICA,
                        7,
                        GRIS_TEXTO
                );

        private final Font fuentePagina =
                FontFactory.getFont(
                        FontFactory.HELVETICA,
                        8,
                        GRIS_TEXTO
                );

        EventoPaginaUMA(Image logo) {
            this.logo = logo;
        }

        @Override
        public void onEndPage(
                PdfWriter writer,
                Document document
        ) {

            PdfContentByte canvas = writer.getDirectContent();

            float ancho = document.getPageSize().getWidth();
            float izquierda = document.leftMargin();
            float derecha = ancho - document.rightMargin();

            // ----------------------------------
            // ENCABEZADO
            // ----------------------------------

            float posicionLogoX = izquierda;
            float posicionLogoY =
                    document.getPageSize().getHeight() - 76;

            if (logo != null) {

                try {

                    Image imagen = Image.getInstance(logo);
                    imagen.scaleToFit(54, 54);

                    imagen.setAbsolutePosition(
                            posicionLogoX,
                            posicionLogoY
                    );

                    canvas.addImage(imagen);

                } catch (Exception ignored) {
                    // El documento puede generarse sin el logotipo.
                }
            }

            float textoX = izquierda + 66;
            float textoY =
                    document.getPageSize().getHeight() - 43;

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_LEFT,
                    new Phrase(
                            "UNIVERSIDAD MODULAR ABIERTA",
                            fuenteInstitucional
                    ),
                    textoX,
                    textoY,
                    0
            );

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_LEFT,
                    new Phrase(
                            "SISTEMA DE REGISTRO ACADÉMICO",
                            fuenteDetalle
                    ),
                    textoX,
                    textoY - 15,
                    0
            );

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_LEFT,
                    new Phrase(
                            "DOCUMENTO ACADÉMICO",
                            FontFactory.getFont(
                                    FontFactory.HELVETICA_BOLD,
                                    7,
                                    ROJO
                            )
                    ),
                    textoX,
                    textoY - 27,
                    0
            );

            canvas.saveState();

            canvas.setColorStroke(ROJO);
            canvas.setLineWidth(2);

            canvas.moveTo(
                    izquierda,
                    document.getPageSize().getHeight() - 88
            );

            canvas.lineTo(
                    derecha,
                    document.getPageSize().getHeight() - 88
            );

            canvas.stroke();
            canvas.restoreState();

            // ----------------------------------
            // PIE DE PÁGINA
            // ----------------------------------

            float footerY = document.bottom() - 20;

            canvas.saveState();

            canvas.setColorStroke(BORDE);
            canvas.setLineWidth(0.7f);

            canvas.moveTo(izquierda, footerY + 13);
            canvas.lineTo(derecha, footerY + 13);

            canvas.stroke();

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_LEFT,
                    new Phrase(
                            "UNIVERSIDAD MODULAR ABIERTA | SRA",
                            fuenteDetalle
                    ),
                    izquierda,
                    footerY,
                    0
            );

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_RIGHT,
                    new Phrase(
                            "Página " + writer.getPageNumber(),
                            fuentePagina
                    ),
                    derecha,
                    footerY,
                    0
            );

            canvas.restoreState();
        }
    }
}
