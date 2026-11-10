
package com.codice.sra.services;

import com.codice.sra.dtos.ComprobantePagoDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ComprobantePdfService {

    @Value("${app.pdf.logo-path:uploads/pdf/uma-logo.jpeg}")
    private String logoPath;

    // Identidad visual UMA
    private static final Color UMA_RED = new Color(200, 16, 46);
    private static final Color DARK = new Color(40, 40, 40);
    private static final Color GRAY = new Color(105, 105, 105);
    private static final Color LIGHT_GRAY = new Color(246, 246, 246);
    private static final Color BORDER = new Color(205, 205, 205);
    private static final Color WHITE = Color.WHITE;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generarPdf(ComprobantePagoDTO comprobante) {

        if (comprobante == null) {
            throw new IllegalArgumentException(
                    "El comprobante no puede ser nulo."
            );
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(
                    PageSize.A4,
                    35,
                    35,
                    32,
                    55
            );

            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PiePaginaEvent());

            document.open();

            // Fuentes
            Font titleFont = font(FontFactory.HELVETICA_BOLD, 17, UMA_RED);
            Font institutionFont = font(FontFactory.HELVETICA_BOLD, 11, DARK);
            Font normalFont = font(FontFactory.HELVETICA, 9, DARK);
            Font boldFont = font(FontFactory.HELVETICA_BOLD, 9, DARK);
            Font labelFont = font(FontFactory.HELVETICA_BOLD, 7, GRAY);
            Font smallFont = font(FontFactory.HELVETICA, 7, GRAY);
            Font tableHeaderFont = font(
                    FontFactory.HELVETICA_BOLD, 8, WHITE
            );

            // =================================================
            // 1. ENCABEZADO INSTITUCIONAL Y DATOS DEL DOCUMENTO
            // =================================================

            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.05f, 1.35f});
            header.setSpacingAfter(12f);

            // Panel izquierdo: institución
            PdfPCell institutionCell = new PdfPCell();
            institutionCell.setBorderColor(BORDER);
            institutionCell.setBorderWidth(0.8f);
            institutionCell.setPadding(10f);
            institutionCell.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );

            try {
                Image logo = cargarLogoRecortado();

                if (logo != null) {
                    logo.scaleToFit(100, 90);
                    logo.setAlignment(Element.ALIGN_CENTER);

                    institutionCell.addElement(logo);
                }
            } catch (Exception e) {
                // Si el logo no está disponible, se conserva el texto.
            }

            Paragraph institutionName = new Paragraph(
                    "UNIVERSIDAD MODULAR ABIERTA",
                    institutionFont
            );
            institutionName.setAlignment(Element.ALIGN_CENTER);
            institutionName.setSpacingBefore(5f);

            Paragraph regional = new Paragraph(
                    "CENTRO REGIONAL DE SANTA ANA",
                    boldFont
            );
            regional.setAlignment(Element.ALIGN_CENTER);
            regional.setSpacingBefore(3f);

            Paragraph address = new Paragraph(
                    "Av. Independencia, Santa Ana, El Salvador",
                    smallFont
            );
            address.setAlignment(Element.ALIGN_CENTER);
            address.setSpacingBefore(7f);

            Paragraph phone = new Paragraph(
                    "Tel.: +503 2219-6503",
                    smallFont
            );
            phone.setAlignment(Element.ALIGN_CENTER);

            Paragraph website = new Paragraph(
                    "www.umasantaana.edu.sv",
                    smallFont
            );
            website.setAlignment(Element.ALIGN_CENTER);

            institutionCell.addElement(institutionName);
            institutionCell.addElement(regional);
            institutionCell.addElement(address);
            institutionCell.addElement(phone);
            institutionCell.addElement(website);

            header.addCell(institutionCell);

            // Panel derecho: identificación del comprobante
            PdfPCell documentCell = new PdfPCell();
            documentCell.setBorderColor(BORDER);
            documentCell.setBorderWidth(0.8f);
            documentCell.setPadding(11f);

            Paragraph documentTitle = new Paragraph(
                    "COMPROBANTE DE PAGO",
                    titleFont
            );
            documentTitle.setAlignment(Element.ALIGN_CENTER);
            documentTitle.setSpacingAfter(3f);

            Paragraph documentSubtitle = new Paragraph(
                    "REGISTRO DE OPERACIÓN ACADÉMICA",
                    smallFont
            );
            documentSubtitle.setAlignment(Element.ALIGN_CENTER);
            documentSubtitle.setSpacingAfter(13f);

            documentCell.addElement(documentTitle);
            documentCell.addElement(documentSubtitle);

            PdfPTable documentInfo = new PdfPTable(2);
            documentInfo.setWidthPercentage(100);
            documentInfo.setWidths(new float[]{1f, 1.3f});

            agregarDatoDocumento(
                    documentInfo,
                    "N.º COMPROBANTE",
                    valorSeguro(comprobante.getNumeroFactura()),
                    labelFont,
                    boldFont
            );

            String fecha = comprobante.getFechaEmision() != null
                    ? comprobante.getFechaEmision().format(DATE_FORMAT)
                    : "No disponible";

            agregarDatoDocumento(
                    documentInfo,
                    "FECHA DE EMISIÓN",
                    fecha,
                    labelFont,
                    boldFont
            );

            PdfPCell typeLabel = new PdfPCell(
                    new Phrase("TIPO DE DOCUMENTO", labelFont)
            );
            typeLabel.setColspan(2);
            typeLabel.setBorder(Rectangle.NO_BORDER);
            typeLabel.setPaddingTop(10f);

            PdfPCell typeValue = new PdfPCell(
                    new Phrase(
                            "Comprobante interno de pago",
                            normalFont
                    )
            );
            typeValue.setColspan(2);
            typeValue.setBorder(Rectangle.NO_BORDER);
            typeValue.setPaddingTop(4f);

            documentInfo.addCell(typeLabel);
            documentInfo.addCell(typeValue);

            documentCell.addElement(documentInfo);
            header.addCell(documentCell);

            document.add(header);

            // Línea institucional
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            line.setSpacingAfter(14f);

            PdfPCell lineCell = new PdfPCell();
            lineCell.setBackgroundColor(UMA_RED);
            lineCell.setBorder(Rectangle.NO_BORDER);
            lineCell.setFixedHeight(3f);

            line.addCell(lineCell);
            document.add(line);

            // =================================================
            // 2. INFORMACIÓN DEL ESTUDIANTE
            // =================================================

            agregarTituloSeccion(
                    document,
                    "INFORMACIÓN DEL ESTUDIANTE",
                    boldFont
            );

            PdfPTable studentTable = new PdfPTable(2);
            studentTable.setWidthPercentage(100);
            studentTable.setWidths(new float[]{1.2f, 1f});
            studentTable.setSpacingAfter(15f);

            PdfPCell nameCell = new PdfPCell();
            nameCell.setColspan(2);
            nameCell.setBorderColor(BORDER);
            nameCell.setBackgroundColor(LIGHT_GRAY);
            nameCell.setPadding(9f);

            nameCell.addElement(new Paragraph(
                    "NOMBRE COMPLETO",
                    labelFont
            ));

            Paragraph studentName = new Paragraph(
                    valorSeguro(comprobante.getNombreCompleto()),
                    boldFont
            );
            studentName.setSpacingBefore(4f);

            nameCell.addElement(studentName);
            studentTable.addCell(nameCell);

            agregarCampo(
                    studentTable,
                    "CARNET ESTUDIANTIL",
                    valorSeguro(comprobante.getCarnetEstudiante()),
                    labelFont,
                    normalFont
            );

            agregarCampo(
                    studentTable,
                    "CORREO ELECTRÓNICO",
                    valorSeguro(comprobante.getCorreoEstudiante()),
                    labelFont,
                    normalFont
            );

            document.add(studentTable);

            // =================================================
            // 3. DETALLE DE LOS CONCEPTOS PAGADOS
            // =================================================

            agregarTituloSeccion(
                    document,
                    "DETALLE DE LOS CONCEPTOS",
                    boldFont
            );

            PdfPTable detailTable = new PdfPTable(3);
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[]{0.45f, 3.8f, 1.1f});
            detailTable.setHeaderRows(1);
            detailTable.setSpacingAfter(12f);

            agregarEncabezadoTabla(
                    detailTable,
                    "N.º",
                    tableHeaderFont
            );

            agregarEncabezadoTabla(
                    detailTable,
                    "DESCRIPCIÓN",
                    tableHeaderFont
            );

            agregarEncabezadoTabla(
                    detailTable,
                    "VALOR (USD)",
                    tableHeaderFont
            );

            if (comprobante.getDetalles() != null
                    && !comprobante.getDetalles().isEmpty()) {

                int index = 1;

                for (ComprobantePagoDTO.DetalleComprobanteDTO detalle
                        : comprobante.getDetalles()) {

                    Color rowColor = index % 2 == 0
                            ? LIGHT_GRAY
                            : WHITE;

                    PdfPCell numberCell = crearCelda(
                            String.valueOf(index),
                            normalFont,
                            rowColor
                    );
                    numberCell.setHorizontalAlignment(
                            Element.ALIGN_CENTER
                    );

                    PdfPCell conceptCell = crearCelda(
                            valorSeguro(detalle.getConcepto()),
                            normalFont,
                            rowColor
                    );

                    PdfPCell amountCell = crearCelda(
                            "$ " + formatoMonto(detalle.getMontoTotal()),
                            boldFont,
                            rowColor
                    );
                    amountCell.setHorizontalAlignment(
                            Element.ALIGN_RIGHT
                    );

                    detailTable.addCell(numberCell);
                    detailTable.addCell(conceptCell);
                    detailTable.addCell(amountCell);

                    index++;
                }

            } else {

                PdfPCell emptyCell = crearCelda(
                        "No hay conceptos registrados.",
                        normalFont,
                        LIGHT_GRAY
                );

                emptyCell.setColspan(3);
                detailTable.addCell(emptyCell);
            }

            document.add(detailTable);

            // =================================================
            // 4. RESUMEN ECONÓMICO
            // =================================================

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{2.2f, 1f});
            summaryTable.setSpacingBefore(4f);
            summaryTable.setSpacingAfter(15f);

            PdfPCell summaryDescription = new PdfPCell();
            summaryDescription.setBorderColor(BORDER);
            summaryDescription.setPadding(10f);

            summaryDescription.addElement(new Paragraph(
                    "RESUMEN DE LA OPERACIÓN",
                    boldFont
            ));

            Paragraph summaryText = new Paragraph(
                    "El presente documento registra los conceptos "
                            + "y el importe recibido por la operación académica.",
                    smallFont
            );
            summaryText.setSpacingBefore(5f);

            summaryDescription.addElement(summaryText);

            PdfPCell totalLabel = new PdfPCell(
                    new Phrase("TOTAL PAGADO", tableHeaderFont)
            );
            totalLabel.setBackgroundColor(UMA_RED);
            totalLabel.setBorder(Rectangle.NO_BORDER);
            totalLabel.setPadding(10f);
            totalLabel.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            PdfPCell totalValue = new PdfPCell(
                    new Phrase(
                            "$ " + formatoMonto(
                                    comprobante.getTotalCobrado()
                            ),
                            font(
                                    FontFactory.HELVETICA_BOLD,
                                    16,
                                    WHITE
                            )
                    )
            );
            totalValue.setBackgroundColor(UMA_RED);
            totalValue.setBorder(Rectangle.NO_BORDER);
            totalValue.setPadding(12f);
            totalValue.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            PdfPTable totalBlock = new PdfPTable(1);
            totalBlock.setWidthPercentage(100);
            totalBlock.addCell(totalLabel);
            totalBlock.addCell(totalValue);

            PdfPCell totalWrapper = new PdfPCell(totalBlock);
            totalWrapper.setBorder(Rectangle.NO_BORDER);
            totalWrapper.setPadding(0);

            summaryTable.addCell(summaryDescription);
            summaryTable.addCell(totalWrapper);

            document.add(summaryTable);

            // =================================================
            // 5. DATOS DEL REGISTRO
            // =================================================

            agregarTituloSeccion(
                    document,
                    "DATOS DEL REGISTRO",
                    boldFont
            );

            PdfPTable responsibleTable = new PdfPTable(2);
            responsibleTable.setWidthPercentage(100);
            responsibleTable.setWidths(new float[]{1f, 2f});
            responsibleTable.setSpacingAfter(15f);

            agregarCampo(
                    responsibleTable,
                    "RESPONSABLE DE CAJA",
                    valorSeguro(comprobante.getCajeroResponsable()),
                    labelFont,
                    normalFont
            );

            agregarCampo(
                    responsibleTable,
                    "INSTITUCIÓN",
                    "Universidad Modular Abierta - Santa Ana",
                    labelFont,
                    normalFont
            );

            document.add(responsibleTable);

            // =================================================
            // 6. MENSAJE FINAL
            // =================================================

            PdfPTable finalTable = new PdfPTable(1);
            finalTable.setWidthPercentage(100);

            PdfPCell finalCell = new PdfPCell();
            finalCell.setBorderColor(BORDER);
            finalCell.setBackgroundColor(LIGHT_GRAY);
            finalCell.setPadding(12f);

            Paragraph thanks = new Paragraph(
                    "GRACIAS POR SU PAGO",
                    font(FontFactory.HELVETICA_BOLD, 10, UMA_RED)
            );
            thanks.setAlignment(Element.ALIGN_CENTER);
            thanks.setSpacingAfter(5f);

            Paragraph message = new Paragraph(
                    "Conserve este documento como respaldo "
                            + "de su operación académica.",
                    smallFont
            );
            message.setAlignment(Element.ALIGN_CENTER);

            finalCell.addElement(thanks);
            finalCell.addElement(message);

            finalTable.addCell(finalCell);
            document.add(finalTable);

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al generar el comprobante de pago PDF.",
                    e
            );
        }
    }

    // =====================================================
    // CARGA Y RECORTE DEL LOGO
    // =====================================================

    private Image cargarLogoRecortado() throws Exception {

        File file = new File(logoPath);

        if (!file.exists() || !file.isFile()) {
            return null;
        }

        BufferedImage original = ImageIO.read(file);

        if (original == null) {
            return Image.getInstance(file.getAbsolutePath());
        }

        int minX = original.getWidth();
        int minY = original.getHeight();
        int maxX = 0;
        int maxY = 0;

        // Detecta el contenido y elimina márgenes blancos.
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {

                Color pixel = new Color(
                        original.getRGB(x, y),
                        true
                );

                boolean visible = pixel.getAlpha() > 20;

                boolean notWhite =
                        pixel.getRed() < 245
                                || pixel.getGreen() < 245
                                || pixel.getBlue() < 245;

                if (visible && notWhite) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX <= minX || maxY <= minY) {
            return Image.getInstance(file.getAbsolutePath());
        }

        int padding = Math.max(
                2,
                (int) (Math.max(maxX - minX, maxY - minY) * 0.04)
        );

        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(original.getWidth() - 1, maxX + padding);
        maxY = Math.min(original.getHeight() - 1, maxY + padding);

        BufferedImage cropped = original.getSubimage(
                minX,
                minY,
                maxX - minX + 1,
                maxY - minY + 1
        );

        ByteArrayOutputStream imageBytes =
                new ByteArrayOutputStream();

        ImageIO.write(cropped, "png", imageBytes);

        return Image.getInstance(imageBytes.toByteArray());
    }

    // =====================================================
    // MÉTODOS AUXILIARES
    // =====================================================

    private Font font(String family, int size, Color color) {
        return FontFactory.getFont(family, size, color);
    }

    private void agregarTituloSeccion(
            Document document,
            String title,
            Font font
    ) throws DocumentException {

        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(7f);
        paragraph.setSpacingAfter(7f);

        document.add(paragraph);
    }

    private void agregarDatoDocumento(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont
    ) {

        PdfPCell labelCell = new PdfPCell(
                new Phrase(label, labelFont)
        );
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5f);

        PdfPCell valueCell = new PdfPCell(
                new Phrase(value, valueFont)
        );
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void agregarCampo(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont
    ) {

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(8f);

        cell.addElement(new Paragraph(label, labelFont));

        Paragraph valueParagraph = new Paragraph(
                value,
                valueFont
        );
        valueParagraph.setSpacingBefore(4f);

        cell.addElement(valueParagraph);

        table.addCell(cell);
    }

    private void agregarEncabezadoTabla(
            PdfPTable table,
            String text,
            Font font
    ) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text, font)
        );

        cell.setBackgroundColor(UMA_RED);
        cell.setBorderColor(WHITE);
        cell.setPadding(9f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private PdfPCell crearCelda(
            String text,
            Font font,
            Color background
    ) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text, font)
        );

        cell.setBackgroundColor(background);
        cell.setBorderColor(BORDER);
        cell.setPadding(9f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        return cell;
    }

    private String valorSeguro(String value) {

        if (value == null
                || value.trim().isEmpty()
                || value.trim().equalsIgnoreCase("N/A")) {

            return "No disponible";
        }

        return value.trim();
    }

    private String formatoMonto(Number amount) {

        if (amount == null) {
            return "0.00";
        }

        DecimalFormatSymbols symbols =
                new DecimalFormatSymbols(Locale.US);

        DecimalFormat format =
                new DecimalFormat("#,##0.00", symbols);

        return format.format(amount);
    }

    // =====================================================
    // PIE DE PÁGINA
    // =====================================================

    private static class PiePaginaEvent extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {

            PdfContentByte canvas = writer.getDirectContent();

            float left = document.left();
            float right = document.right();
            float y = document.bottom() - 23f;

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
                            "UMA Santa Ana | Sistema de Registro Académico",
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
                                    + " | Prototipo administrativo",
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
