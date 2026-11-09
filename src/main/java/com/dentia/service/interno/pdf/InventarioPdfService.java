package com.dentia.service.interno.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class InventarioPdfService {

    public byte[] generarPdfTabla(
            String titulo,
            String subtitulo,
            String[] columnas,
            List<String[]> filas
    ) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();

            Document documento = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
            com.lowagie.text.pdf.PdfWriter.getInstance(documento, salida);

            documento.open();

            agregarEncabezado(documento, titulo, subtitulo);
            agregarTabla(documento, columnas, filas);
            agregarPie(documento);

            documento.close();

            return salida.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el PDF.");
        }
    }

    public byte[] generarPdfDetalle(
            String titulo,
            String subtitulo,
            List<String[]> datos
    ) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();

            Document documento = new Document(PageSize.A4, 36, 36, 36, 36);
            com.lowagie.text.pdf.PdfWriter.getInstance(documento, salida);

            documento.open();

            agregarEncabezado(documento, titulo, subtitulo);
            agregarTablaDetalle(documento, datos);
            agregarPie(documento);

            documento.close();

            return salida.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el PDF detalle.");
        }
    }

    private void agregarEncabezado(Document documento, String titulo, String subtitulo) throws DocumentException {
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph empresa = new Paragraph("DentIA", tituloFont);
        empresa.setSpacingAfter(4);
        documento.add(empresa);

        Paragraph tituloReporte = new Paragraph(valor(titulo), tituloFont);
        tituloReporte.setSpacingAfter(4);
        documento.add(tituloReporte);

        Paragraph subtituloReporte = new Paragraph(valor(subtitulo), subtituloFont);
        subtituloReporte.setSpacingAfter(14);
        documento.add(subtituloReporte);
    }

    private void agregarTabla(Document documento, String[] columnas, List<String[]> filas) throws DocumentException {
        Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Font tdFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPTable tabla = new PdfPTable(columnas.length);
        tabla.setWidthPercentage(100);

        for (String columna : columnas) {
            PdfPCell celda = new PdfPCell(new Phrase(valor(columna), thFont));
            celda.setBackgroundColor(new Color(243, 244, 246));
            celda.setPadding(6);
            tabla.addCell(celda);
        }

        for (String[] fila : filas) {
            for (String dato : fila) {
                PdfPCell celda = new PdfPCell(new Phrase(valor(dato), tdFont));
                celda.setPadding(5);
                tabla.addCell(celda);
            }
        }

        documento.add(tabla);
    }

    private void agregarTablaDetalle(Document documento, List<String[]> datos) throws DocumentException {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.4f, 3f});

        for (String[] dato : datos) {
            PdfPCell label = new PdfPCell(new Phrase(valor(dato[0]), labelFont));
            label.setBackgroundColor(new Color(243, 244, 246));
            label.setPadding(8);

            PdfPCell value = new PdfPCell(new Phrase(valor(dato[1]), valueFont));
            value.setPadding(8);

            tabla.addCell(label);
            tabla.addCell(value);
        }

        documento.add(tabla);
    }

    private void agregarPie(Document documento) throws DocumentException {
        Font pieFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        Paragraph pie = new Paragraph("Documento generado desde el sistema DentIA.", pieFont);
        pie.setSpacingBefore(16);
        pie.setAlignment(Element.ALIGN_CENTER);

        documento.add(pie);
    }

    private String valor(String texto) {
        return texto == null || texto.trim().isEmpty() ? "-" : texto;
    }
}