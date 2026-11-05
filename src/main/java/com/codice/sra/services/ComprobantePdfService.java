package com.codice.sra.services;

import com.codice.sra.dtos.ComprobantePagoDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ComprobantePdfService {

    public byte[] generarPdf(ComprobantePagoDTO comprobante) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            // Colores Institucionales (Rojo UMA, Negro y Grises)
            Color umaRed = new Color(179, 0, 0); // Rojo oscuro institucional
            Color black = new Color(33, 33, 33);
            Color darkGray = new Color(100, 100, 100);
            Color lightGray = new Color(240, 240, 240);
            Color white = Color.WHITE;

            // Fuentes
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, umaRed);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, white);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10, black);
            Font fontNormalGray = FontFactory.getFont(FontFactory.HELVETICA, 10, darkGray);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, black);

            // ==========================================
            // 1. CABECERA: Logo e Información vs. Título de Factura
            // ==========================================
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1.2f, 1f});

            // Lado Izquierdo: Logo y Datos de la Universidad
            PdfPCell leftHeaderCell = new PdfPCell();
            leftHeaderCell.setBorder(Rectangle.NO_BORDER);

            try {
                // Carga la imagen directamente desde la URL
                Image logo = Image.getInstance("https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png");
                logo.scaleToFit(140, 50);
                logo.setAlignment(Element.ALIGN_LEFT);
                leftHeaderCell.addElement(logo);
            } catch (Exception e) {
                Paragraph altText = new Paragraph("UNIVERSIDAD MODULAR ABIERTA", fontBold);
                leftHeaderCell.addElement(altText);
            }

            leftHeaderCell.addElement(new Paragraph(" ", fontNormal)); // Espaciador
            leftHeaderCell.addElement(new Paragraph("Universidad Modular Abierta", fontBold));
            leftHeaderCell.addElement(new Paragraph("Centro Regional de Santa Ana", fontNormalGray));
            leftHeaderCell.addElement(new Paragraph("www.uma.edu.sv", fontNormalGray));

            headerTable.addCell(leftHeaderCell);

            // Lado Derecho: Título y Cuadro de Número/Fecha
            PdfPCell rightHeaderCell = new PdfPCell();
            rightHeaderCell.setBorder(Rectangle.NO_BORDER);
            rightHeaderCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph title = new Paragraph("FACTURA\n\n", fontTitle);
            title.setAlignment(Element.ALIGN_RIGHT);
            rightHeaderCell.addElement(title);

            // Sub-tabla para N° Factura y Fecha (Diseño azul/blanco de la plantilla convertido a rojo)
            PdfPTable infoInvoiceTable = new PdfPTable(2);
            infoInvoiceTable.setWidthPercentage(100);

            // Fila 1: Encabezados rojos
            PdfPCell hNro = new PdfPCell(new Phrase("N.º DE FACTURA", fontHeader));
            hNro.setBackgroundColor(umaRed);
            hNro.setBorder(Rectangle.NO_BORDER);
            hNro.setPadding(6f);
            hNro.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell hFecha = new PdfPCell(new Phrase("FECHA", fontHeader));
            hFecha.setBackgroundColor(umaRed);
            hFecha.setBorder(Rectangle.NO_BORDER);
            hFecha.setPadding(6f);
            hFecha.setHorizontalAlignment(Element.ALIGN_CENTER);

            // Fila 2: Valores
            PdfPCell vNro = new PdfPCell(new Phrase(comprobante.getNumeroFactura(), fontNormal));
            vNro.setBorder(Rectangle.NO_BORDER);
            vNro.setPadding(6f);
            vNro.setHorizontalAlignment(Element.ALIGN_CENTER);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            PdfPCell vFecha = new PdfPCell(new Phrase(comprobante.getFechaEmision().format(formatter), fontNormal));
            vFecha.setBorder(Rectangle.NO_BORDER);
            vFecha.setPadding(6f);
            vFecha.setHorizontalAlignment(Element.ALIGN_CENTER);

            infoInvoiceTable.addCell(hNro);
            infoInvoiceTable.addCell(hFecha);
            infoInvoiceTable.addCell(vNro);
            infoInvoiceTable.addCell(vFecha);

            rightHeaderCell.addElement(infoInvoiceTable);
            headerTable.addCell(rightHeaderCell);

            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // ==========================================
            // 2. SECCIÓN: FACTURAR A (Datos del Estudiante)
            // ==========================================
            PdfPTable billToTable = new PdfPTable(2);
            billToTable.setWidthPercentage(100);
            billToTable.setWidths(new float[]{1f, 1f});

            // Lado Izquierdo: Datos
            PdfPCell clientCell = new PdfPCell();
            clientCell.setBorder(Rectangle.NO_BORDER);

            PdfPCell clientHeader = new PdfPCell(new Phrase("FACTURAR A:", fontHeader));
            clientHeader.setBackgroundColor(umaRed);
            clientHeader.setBorder(Rectangle.NO_BORDER);
            clientHeader.setPadding(5f);

            PdfPTable innerClientTable = new PdfPTable(1);
            innerClientTable.setWidthPercentage(100);
            innerClientTable.addCell(clientHeader);

            PdfPCell clientData = new PdfPCell();
            clientData.setBorder(Rectangle.NO_BORDER);
            clientData.setPaddingTop(5f);
            clientData.addElement(new Paragraph(comprobante.getNombreCompleto(), fontBold));
            clientData.addElement(new Paragraph("Carnet: " + comprobante.getCarnetEstudiante(), fontNormal));
            if (comprobante.getCorreoEstudiante() != null && !comprobante.getCorreoEstudiante().equals("N/A")) {
                clientData.addElement(new Paragraph(comprobante.getCorreoEstudiante(), fontNormal));
            }
            innerClientTable.addCell(clientData);

            clientCell.addElement(innerClientTable);
            billToTable.addCell(clientCell);

            // Lado Derecho: Vacío para equilibrar el diseño
            PdfPCell emptyHeaderCell = new PdfPCell();
            emptyHeaderCell.setBorder(Rectangle.NO_BORDER);
            billToTable.addCell(emptyHeaderCell);

            document.add(billToTable);
            document.add(new Paragraph("\n\n"));

            // ==========================================
            // 3. TABLA DE DETALLES
            // ==========================================
            PdfPTable itemsTable = new PdfPTable(2);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{4f, 1f});

            // Encabezados de tabla
            PdfPCell thDesc = new PdfPCell(new Phrase("DESCRIPCIÓN", fontHeader));
            thDesc.setBackgroundColor(umaRed);
            thDesc.setBorder(Rectangle.NO_BORDER);
            thDesc.setPadding(8f);

            PdfPCell thMonto = new PdfPCell(new Phrase("MONTO", fontHeader));
            thMonto.setBackgroundColor(umaRed);
            thMonto.setBorder(Rectangle.NO_BORDER);
            thMonto.setPadding(8f);
            thMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);

            itemsTable.addCell(thDesc);
            itemsTable.addCell(thMonto);

            // Filas dinámicas
            for (ComprobantePagoDTO.DetalleComprobanteDTO detalle : comprobante.getDetalles()) {
                PdfPCell cellDesc = new PdfPCell(new Phrase(detalle.getConcepto(), fontNormal));
                cellDesc.setBorder(Rectangle.BOTTOM);
                cellDesc.setBorderColor(lightGray);
                cellDesc.setPadding(8f);

                PdfPCell cellMontoVal = new PdfPCell(new Phrase(String.format("%.2f", detalle.getMontoTotal()), fontNormal));
                cellMontoVal.setBorder(Rectangle.BOTTOM);
                cellMontoVal.setBorderColor(lightGray);
                cellMontoVal.setPadding(8f);
                cellMontoVal.setHorizontalAlignment(Element.ALIGN_RIGHT);

                itemsTable.addCell(cellDesc);
                itemsTable.addCell(cellMontoVal);
            }

            // Filas vacías de relleno (para simular el diseño de la plantilla)
            for (int i = 0; i < 3; i++) {
                PdfPCell emptyRow = new PdfPCell(new Phrase(" ", fontNormal));
                emptyRow.setBorder(Rectangle.BOTTOM);
                emptyRow.setBorderColor(lightGray);
                emptyRow.setPadding(8f);
                itemsTable.addCell(emptyRow);
                itemsTable.addCell(emptyRow);
            }
            document.add(itemsTable);

            // ==========================================
            // 4. PIE Y TOTALES
            // ==========================================
            PdfPTable footerLayout = new PdfPTable(2);
            footerLayout.setWidthPercentage(100);
            footerLayout.setWidths(new float[]{2.5f, 1.5f});
            footerLayout.setSpacingBefore(10f);

            // Observaciones (Izquierda)
            PdfPCell obsCell = new PdfPCell();
            obsCell.setBorder(Rectangle.NO_BORDER);
            obsCell.addElement(new Paragraph("Observaciones/Instrucciones:", fontNormalGray));
            obsCell.addElement(new Paragraph("Atendido por: " + comprobante.getCajeroResponsable(), fontNormal));
            obsCell.addElement(new Paragraph("Comprobante válido de pago electrónico.", fontNormal));

            Paragraph gracias = new Paragraph("\nGRACIAS", fontTitle);
            gracias.setAlignment(Element.ALIGN_CENTER);
            obsCell.addElement(gracias);

            footerLayout.addCell(obsCell);

            // Totales (Derecha)
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(100);
            totalsTable.setWidths(new float[]{1.5f, 1f});

            // Fila Total
            PdfPCell lblTotal = new PdfPCell(new Phrase("TOTAL", fontBold));
            lblTotal.setBackgroundColor(lightGray);
            lblTotal.setBorder(Rectangle.BOTTOM | Rectangle.TOP);
            lblTotal.setBorderColor(Color.WHITE);
            lblTotal.setPadding(8f);

            PdfPCell valTotalCell = new PdfPCell(new Phrase("$ " + String.format("%.2f", comprobante.getTotalCobrado()), fontBold));
            valTotalCell.setBackgroundColor(lightGray);
            valTotalCell.setBorder(Rectangle.BOTTOM | Rectangle.TOP);
            valTotalCell.setBorderColor(Color.WHITE);
            valTotalCell.setPadding(8f);
            valTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            totalsTable.addCell(lblTotal);
            totalsTable.addCell(valTotalCell);

            PdfPCell totalsWrapper = new PdfPCell(totalsTable);
            totalsWrapper.setBorder(Rectangle.NO_BORDER);
            footerLayout.addCell(totalsWrapper);

            document.add(footerLayout);
            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al dibujar el PDF del comprobante", e);
        }
    }
}