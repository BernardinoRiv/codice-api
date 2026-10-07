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

            Document document = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            Color primaryColor = new Color(33, 33, 33);
            Color secondaryColor = new Color(100, 100, 100);
            Color accentColor = new Color(245, 245, 240);

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, primaryColor);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryColor);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10, secondaryColor);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryColor);

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1f, 1f});

            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.addElement(new Paragraph("FACTURA", fontTitle));

            PdfPTable nroTable = new PdfPTable(1);
            nroTable.setWidthPercentage(70);
            nroTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            PdfPCell nroCell = new PdfPCell(new Phrase("Nº: " + comprobante.getNumeroFactura(), fontBold));
            nroCell.setBackgroundColor(accentColor);
            nroCell.setPadding(8);
            nroCell.setBorder(Rectangle.BOX);
            nroCell.setBorderWidth(1.5f);
            nroTable.addCell(nroCell);

            titleCell.addElement(Chunk.NEWLINE);
            titleCell.addElement(nroTable);
            headerTable.addCell(titleCell);

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            try {
                Image logo = Image.getInstance("https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png");
                logo.scaleToFit(160, 60);
                logo.setAlignment(Element.ALIGN_RIGHT);
                logoCell.addElement(logo);
            } catch (Exception e) {
                Paragraph altText = new Paragraph("UMA", fontTitle);
                altText.setAlignment(Element.ALIGN_RIGHT);
                logoCell.addElement(altText);
            }
            headerTable.addCell(logoCell);
            document.add(headerTable);

            document.add(new Paragraph("\n\n"));

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1f, 1f});

            PdfPCell clienteCell = new PdfPCell();
            clienteCell.setBorder(Rectangle.NO_BORDER);
            clienteCell.addElement(new Paragraph("DATOS DEL ESTUDIANTE", fontHeader));
            clienteCell.addElement(new Paragraph(comprobante.getNombreCompleto(), fontNormal));
            clienteCell.addElement(new Paragraph("Carnet: " + comprobante.getCarnetEstudiante(), fontNormal));
            if (comprobante.getCorreoEstudiante() != null && !comprobante.getCorreoEstudiante().equals("N/A")) {
                clienteCell.addElement(new Paragraph(comprobante.getCorreoEstudiante(), fontNormal));
            }
            infoTable.addCell(clienteCell);

            PdfPCell empresaCell = new PdfPCell();
            empresaCell.setBorder(Rectangle.LEFT);
            empresaCell.setBorderColor(Color.LIGHT_GRAY);
            empresaCell.setBorderWidthLeft(1.5f);
            empresaCell.setPaddingLeft(20f);
            empresaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph pEmpresaTitle = new Paragraph("UNIVERSIDAD MODULAR ABIERTA", fontHeader);
            pEmpresaTitle.setAlignment(Element.ALIGN_RIGHT);
            empresaCell.addElement(pEmpresaTitle);

            Paragraph pEmpresaSub = new Paragraph("Centro Regional de Santa Ana", fontNormal);
            pEmpresaSub.setAlignment(Element.ALIGN_RIGHT);
            empresaCell.addElement(pEmpresaSub);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            Paragraph pFecha = new Paragraph("Emisión: " + comprobante.getFechaEmision().format(formatter), fontNormal);
            pFecha.setAlignment(Element.ALIGN_RIGHT);
            empresaCell.addElement(pFecha);

            infoTable.addCell(empresaCell);
            document.add(infoTable);

            document.add(new Paragraph("\n\n"));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 1f});

            PdfPCell h1 = new PdfPCell(new Phrase("Detalle", fontHeader));
            h1.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
            h1.setBorderWidth(1.5f);
            h1.setPaddingTop(8f);
            h1.setPaddingBottom(8f);

            PdfPCell h2 = new PdfPCell(new Phrase("Total", fontHeader));
            h2.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
            h2.setBorderWidth(1.5f);
            h2.setPaddingTop(8f);
            h2.setPaddingBottom(8f);
            h2.setHorizontalAlignment(Element.ALIGN_RIGHT);

            table.addCell(h1);
            table.addCell(h2);

            for (ComprobantePagoDTO.DetalleComprobanteDTO detalle : comprobante.getDetalles()) {
                PdfPCell cellConcepto = new PdfPCell(new Phrase(detalle.getConcepto(), fontNormal));
                cellConcepto.setBorder(Rectangle.NO_BORDER);
                cellConcepto.setPaddingTop(12f);
                cellConcepto.setPaddingBottom(12f);

                PdfPCell cellMonto = new PdfPCell(new Phrase("$" + detalle.getMontoTotal().toString(), fontNormal));
                cellMonto.setBorder(Rectangle.NO_BORDER);
                cellMonto.setPaddingTop(12f);
                cellMonto.setPaddingBottom(12f);
                cellMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);

                table.addCell(cellConcepto);
                table.addCell(cellMonto);
            }

            PdfPCell separator = new PdfPCell();
            separator.setColspan(2);
            separator.setBorder(Rectangle.TOP);
            separator.setBorderColor(Color.LIGHT_GRAY);
            separator.setBorderWidthTop(1.5f);
            separator.setPaddingTop(15f);
            table.addCell(separator);

            PdfPCell emptyCell = new PdfPCell();
            emptyCell.setBorder(Rectangle.NO_BORDER);

            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{1f, 1f});

            PdfPCell textTotal = new PdfPCell(new Phrase("TOTAL", fontBold));
            textTotal.setBorder(Rectangle.BOX);
            textTotal.setBorderWidth(1.5f);
            textTotal.setPadding(10f);

            PdfPCell valTotal = new PdfPCell(new Phrase("$" + comprobante.getTotalCobrado().toString(), fontBold));
            valTotal.setBorder(Rectangle.BOX);
            valTotal.setBorderWidth(1.5f);
            valTotal.setPadding(10f);
            valTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);

            totalTable.addCell(textTotal);
            totalTable.addCell(valTotal);

            PdfPCell outerTotalCell = new PdfPCell(totalTable);
            outerTotalCell.setBorder(Rectangle.NO_BORDER);
            outerTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            table.addCell(emptyCell);
            table.addCell(outerTotalCell);

            document.add(table);

            document.add(new Paragraph("\n\n"));

            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(45);
            footerTable.setHorizontalAlignment(Element.ALIGN_LEFT);

            PdfPCell footerCell = new PdfPCell();
            footerCell.setBorder(Rectangle.BOX);
            footerCell.setBorderWidth(1.5f);
            footerCell.setPadding(15f);

            footerCell.addElement(new Paragraph("INFORMACIÓN DE PAGO", fontHeader));
            footerCell.addElement(new Paragraph(" ", fontNormal));
            footerCell.addElement(new Paragraph("Atendido por: " + comprobante.getCajeroResponsable(), fontNormal));
            footerCell.addElement(new Paragraph("Documento válido como comprobante oficial de pago electrónico de la Universidad Modular Abierta.", fontNormal));

            footerTable.addCell(footerCell);
            document.add(footerTable);

            document.add(new Paragraph("\n\n"));
            Paragraph website = new Paragraph("WWW.UMA.EDU.SV", fontBold);
            website.setAlignment(Element.ALIGN_CENTER);
            document.add(website);

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al dibujar el PDF del comprobante", e);
        }
    }
}