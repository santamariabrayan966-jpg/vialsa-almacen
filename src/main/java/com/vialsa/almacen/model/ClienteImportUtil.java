package com.vialsa.almacen.util;

import com.vialsa.almacen.model.Cliente;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteImportUtil {

    // ======================================================
    //           LECTURA CSV PRO
    // ======================================================
    public static List<Cliente> leerCsv(InputStream input) throws IOException {
        List<Cliente> lista = new ArrayList<>();

        BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String linea;
        boolean primera = true;

        while ((linea = br.readLine()) != null) {

            if (primera) { // saltar cabecera
                primera = false;
                continue;
            }

            String[] c = linea.split(",");

            Cliente cli = new Cliente();
            cli.setNombres(c[0]);
            cli.setApellidos(c[1]);
            cli.setNro_documento(c[2]);
            cli.setDireccion(c[3]);
            cli.setTelefono(c[4]);
            cli.setCorreo(c[5]);
            cli.setIdTipoDocumento(Integer.parseInt(c[6]));
            cli.setIdUsuario(Integer.parseInt(c[7]));
            cli.setVip(Integer.parseInt(c[8]) == 1);
            cli.setMoroso(Integer.parseInt(c[9]) == 1);
            cli.setActivo(Integer.parseInt(c[10]) == 1);
            cli.setFoto(c[11]);

            lista.add(cli);
        }

        return lista;
    }


    // ======================================================
    //           LECTURA EXCEL .XLSX PRO
    // ======================================================
    public static List<Cliente> leerExcel(InputStream input) throws Exception {

        List<Cliente> lista = new ArrayList<>();

        Workbook workbook = new XSSFWorkbook(input);
        Sheet sheet = workbook.getSheetAt(0);

        boolean primera = true;

        for (Row fila : sheet) {

            if (primera) {
                primera = false;
                continue;
            }

            Cliente cli = new Cliente();

            cli.setNombres(getValor(fila, 0));
            cli.setApellidos(getValor(fila, 1));
            cli.setNro_documento(getValor(fila, 2));
            cli.setDireccion(getValor(fila, 3));
            cli.setTelefono(getValor(fila, 4));
            cli.setCorreo(getValor(fila, 5));

            cli.setIdTipoDocumento(parseIntSafe(getValor(fila, 6)));
            cli.setIdUsuario(parseIntSafe(getValor(fila, 7)));

            cli.setVip(parseIntSafe(getValor(fila, 8)) == 1);
            cli.setMoroso(parseIntSafe(getValor(fila, 9)) == 1);
            cli.setActivo(parseIntSafe(getValor(fila, 10)) == 1);

            cli.setFoto(getValor(fila, 11));

            lista.add(cli);
        }

        workbook.close();
        return lista;
    }


    // ======================================================
    // HELPERS
    // ======================================================
    private static String getValor(Row row, int col) {
        try {
            Cell cell = row.getCell(col);
            if (cell == null) return "";

            if (cell.getCellType() == CellType.NUMERIC)
                return String.valueOf((int) cell.getNumericCellValue());
            else
                return cell.getStringCellValue().trim();

        } catch (Exception e) {
            return "";
        }
    }

    private static int parseIntSafe(String txt) {
        try {
            return Integer.parseInt(txt);
        } catch (Exception e) {
            return 0;
        }
    }
}
