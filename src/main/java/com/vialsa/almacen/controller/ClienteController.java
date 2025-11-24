package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Cliente;
import com.vialsa.almacen.service.ClienteService;
import com.vialsa.almacen.util.ClienteImportUtil;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// OPENPDF (compatible con Jakarta / Spring Boot 3)
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import org.springframework.validation.BindingResult;




import java.io.OutputStream;
import java.util.List;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // ============================================================
    // LISTADO + DASHBOARD
    // ============================================================
    @GetMapping
    public String listarClientes(Model model) {

        model.addAttribute("clientes", clienteService.listarTodos());

        model.addAttribute("totalClientes", clienteService.listarTodos().size());
        model.addAttribute("clientesNuevos", clienteService.filtrar("nuevo").size());
        model.addAttribute("clientesInactivos", clienteService.filtrar("inactivo").size());
        model.addAttribute("clientesVip", clienteService.filtrar("vip").size());

        return "clientes/clientes";
    }

    // ============================================================
    // BÚSQUEDA AJAX
    // ============================================================
    @GetMapping("/buscar")
    public String buscar(@RequestParam("q") String q, Model model) {
        model.addAttribute("clientes", clienteService.buscar(q));
        return "clientes/clientes :: tablaClientes";
    }

    // ============================================================
    // FILTROS
    // ============================================================
    @GetMapping("/filtro/{tipo}")
    public String filtrar(@PathVariable String tipo, Model model) {
        model.addAttribute("clientes", clienteService.filtrar(tipo));
        return "clientes/clientes :: tablaClientes";
    }

    // ============================================================
    // PERFIL (MODAL)
    // ============================================================
    @GetMapping("/perfil/{id}")
    public String perfil(@PathVariable Integer id, Model model) {
        Cliente c = clienteService.obtenerPerfilCompleto(id);
        model.addAttribute("c", c);
        return "clientes/perfil :: perfilContenido";
    }

    // ============================================================
    // NOTAS
    // ============================================================
    @GetMapping("/notas/{id}")
    @ResponseBody
    public List<String> obtenerNotas(@PathVariable Integer id) {
        return clienteService.obtenerNotas(id);
    }

    @PostMapping("/notas/agregar/{id}")
    @ResponseBody
    public String agregarNota(@PathVariable Integer id, @RequestBody String nota) {
        clienteService.agregarNota(id, nota);
        clienteService.registrarHistorial(id, "Se agregó una nota.");
        return "ok";
    }

    // ============================================================
    // HISTORIAL
    // ============================================================
    @GetMapping("/historial/{id}")
    @ResponseBody
    public List<String> historial(@PathVariable Integer id) {
        return clienteService.obtenerHistorial(id);
    }

    // ============================================================
    // VIP & MOROSO
    // ============================================================
    @PostMapping("/vip/{id}")
    @ResponseBody
    public String marcarVip(@PathVariable Integer id) {
        clienteService.marcarVip(id);
        clienteService.registrarHistorial(id, "Marcado como VIP.");
        return "ok";
    }

    @PostMapping("/vip/quitar/{id}")
    @ResponseBody
    public String quitarVip(@PathVariable Integer id) {
        clienteService.quitarVip(id);
        clienteService.registrarHistorial(id, "Quitado estado VIP.");
        return "ok";
    }

    @PostMapping("/moroso/{id}")
    @ResponseBody
    public String marcarMoroso(@PathVariable Integer id) {
        clienteService.marcarMoroso(id);
        clienteService.registrarHistorial(id, "Marcado como moroso.");
        return "ok";
    }

    @PostMapping("/moroso/quitar/{id}")
    @ResponseBody
    public String quitarMoroso(@PathVariable Integer id) {
        clienteService.quitarMoroso(id);
        clienteService.registrarHistorial(id, "Quitado estado moroso.");
        return "ok";
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR
    // ============================================================
    @PostMapping("/activar/{id}")
    @ResponseBody
    public String activar(@PathVariable Integer id) {
        clienteService.activarCliente(id);
        clienteService.registrarHistorial(id, "Cliente activado.");
        return "ok";
    }

    @PostMapping("/desactivar/{id}")
    @ResponseBody
    public String desactivar(@PathVariable Integer id) {
        clienteService.desactivarCliente(id);
        clienteService.registrarHistorial(id, "Cliente desactivado.");
        return "ok";
    }

    // ============================================================
    // IMPORTAR (CSV / EXCEL PRO)
    // ============================================================
    @PostMapping("/importar")
    public @ResponseBody String importarClientes(@RequestParam("archivo") MultipartFile archivo) {

        try {
            if (archivo.isEmpty()) {
                return "ERROR: archivo vacío";
            }

            String nombre = archivo.getOriginalFilename().toLowerCase();
            List<Cliente> clientesImportados;

            if (nombre.endsWith(".csv")) {
                clientesImportados = ClienteImportUtil.leerCsv(archivo.getInputStream());
            } else if (nombre.endsWith(".xlsx")) {
                clientesImportados = ClienteImportUtil.leerExcel(archivo.getInputStream());
            } else {
                return "Formato no soportado. Usa CSV o Excel (.xlsx)";
            }

            int registrados = clienteService.registrarMasivo(clientesImportados);
            return "Importación exitosa: " + registrados + " clientes.";

        } catch (Exception e) {
            return "Error al importar: " + e.getMessage();
        }
    }

    // ============================================================
    // EXPORTAR (CSV, EXCEL, PDF)
    // ============================================================
    @GetMapping("/exportar/{tipo}")
    public void exportarClientes(@PathVariable String tipo,
                                 HttpServletResponse response) throws Exception {

        List<Cliente> clientes = clienteService.listarParaExportar();

        switch (tipo.toLowerCase()) {
            case "excel":
                exportarExcel(clientes, response);
                break;
            case "csv":
                exportarCsv(clientes, response);
                break;
            case "pdf":
                exportarPdf(clientes, response);
                break;
            default:
                throw new RuntimeException("Tipo de exportación no soportado");
        }
    }

    // ============================================================
    // CSV
    // ============================================================
    private void exportarCsv(List<Cliente> clientes,
                             HttpServletResponse response) throws Exception {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=clientes.csv");

        OutputStream os = response.getOutputStream();

        String header = "ID,Nombres,Apellidos,DNI,Correo,Telefono,VIP,Moroso,Activo\n";
        os.write(header.getBytes());

        for (Cliente c : clientes) {
            String linea = String.format(
                    "%d,%s,%s,%s,%s,%s,%d,%d,%d\n",
                    c.getIdClientes(),
                    c.getNombres(),
                    c.getApellidos(),
                    c.getNro_documento(),
                    c.getCorreo(),
                    c.getTelefono(),
                    c.isVip() ? 1 : 0,
                    c.isMoroso() ? 1 : 0,
                    c.isActivo() ? 1 : 0
            );
            os.write(linea.getBytes());
        }

        os.flush();
    }

    // ============================================================
    // EXCEL
    // ============================================================
    private void exportarExcel(List<Cliente> clientes,
                               HttpServletResponse response) throws Exception {

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=clientes.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Clientes");

        Row header = sheet.createRow(0);
        String[] columnas = {"ID", "Nombres", "Apellidos", "DNI",
                "Correo", "Telefono", "VIP", "Moroso", "Activo"};

        for (int i = 0; i < columnas.length; i++) {
            header.createCell(i).setCellValue(columnas[i]);
        }

        int fila = 1;
        for (Cliente c : clientes) {
            Row row = sheet.createRow(fila++);

            row.createCell(0).setCellValue(c.getIdClientes());
            row.createCell(1).setCellValue(c.getNombres());
            row.createCell(2).setCellValue(c.getApellidos());
            row.createCell(3).setCellValue(c.getNro_documento());
            row.createCell(4).setCellValue(c.getCorreo());
            row.createCell(5).setCellValue(c.getTelefono());
            row.createCell(6).setCellValue(c.isVip() ? "Sí" : "No");
            row.createCell(7).setCellValue(c.isMoroso() ? "Sí" : "No");
            row.createCell(8).setCellValue(c.isActivo() ? "Activo" : "Inactivo");
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ============================================================
    // PDF (OpenPDF)
    // ============================================================
    private void exportarPdf(List<Cliente> clientes,
                             HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=clientes.pdf");

        Document documento = new Document(PageSize.A4);
        PdfWriter.getInstance(documento, response.getOutputStream());

        documento.open();

        // ===== TÍTULO =====
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Paragraph titulo = new Paragraph("Lista de Clientes", tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(20);
        documento.add(titulo);

        // ===== TABLA =====
        PdfPTable tabla = new PdfPTable(5); // 5 columnas
        tabla.setWidthPercentage(100);

        tabla.addCell("ID");
        tabla.addCell("Nombres");
        tabla.addCell("DNI");
        tabla.addCell("Correo");
        tabla.addCell("Teléfono");

        for (Cliente c : clientes) {
            tabla.addCell(String.valueOf(c.getIdClientes()));
            tabla.addCell(c.getNombres() + " " + c.getApellidos());
            tabla.addCell(c.getNro_documento());
            tabla.addCell(c.getCorreo());
            tabla.addCell(c.getTelefono());
        }

        documento.add(tabla);
        documento.close();
    }
    // ============================================================
// FORMULARIO NUEVO CLIENTE
// ============================================================
    @GetMapping("/nuevo")
    public String nuevoCliente(Model model,
                               @RequestParam(value = "fragmento", required = false) Boolean fragmento) {

        Cliente cliente = new Cliente();
        cliente.setActivo(true);

        model.addAttribute("cliente", cliente);

        if (Boolean.TRUE.equals(fragmento)) {
            return "clientes/form-cliente :: formCliente";
        }

        return "clientes/form-cliente";
    }


    // ============================================================
// FORMULARIO EDITAR CLIENTE
// ============================================================
    @GetMapping("/editar/{id}")
    public String editarCliente(@PathVariable Integer id,
                                Model model,
                                @RequestParam(value = "fragmento", required = false) Boolean fragmento) {

        Cliente cliente = clienteService.buscarPorId(id);
        if (cliente == null) return "redirect:/clientes?error=NoExiste";

        model.addAttribute("cliente", cliente);

        if (Boolean.TRUE.equals(fragmento)) {
            return "clientes/form-cliente :: formCliente";
        }

        return "clientes/form-cliente";
    }

    // ============================================================
// GUARDAR CLIENTE (CREAR O ACTUALIZAR)
// ============================================================
    @PostMapping("/guardar")
    public String guardarCliente(
            @ModelAttribute("cliente") Cliente cliente,
            BindingResult result,
            @RequestParam(value = "fileFoto", required = false) MultipartFile archivoFoto,
            Model model
    ) {

        // VALIDACIONES
        if (cliente.getNro_documento() == null || cliente.getNro_documento().isBlank()) {
            result.rejectValue("nro_documento", "dni.vacio", "El DNI es obligatorio");
        }

        if (cliente.getNombres() == null || cliente.getNombres().isBlank()) {
            result.rejectValue("nombres", "nombres.vacio", "Ingrese los nombres");
        }

        if (cliente.getApellidos() == null || cliente.getApellidos().isBlank()) {
            result.rejectValue("apellidos", "apellidos.vacio", "Ingrese los apellidos");
        }

        // ❗ SI HAY ERRORES (cualquier tipo)
        if (result.hasErrors()) {

            // cargar lista de clientes nuevamente para renderizar la tabla
            model.addAttribute("clientes", clienteService.listarTodos());

            model.addAttribute("cliente", cliente);
            model.addAttribute("abrirModalCliente", true);

            return "clientes/clientes";
        }

        try {
            // SUBIR FOTO
            if (archivoFoto != null && !archivoFoto.isEmpty()) {

                String nombreArchivo = System.currentTimeMillis() + "-" + archivoFoto.getOriginalFilename();

                Path ruta = Paths.get("src/main/resources/static/uploads/" + nombreArchivo);

                Files.write(ruta, archivoFoto.getBytes());

                cliente.setFoto(nombreArchivo);
            }

            // CREAR / ACTUALIZAR
            if (cliente.getIdClientes() == null || cliente.getIdClientes() == 0) {
                clienteService.crear(cliente);
            } else {
                clienteService.actualizar(cliente);
            }

            return "redirect:/clientes?exito";

        } catch (org.springframework.dao.DuplicateKeyException ex) {

            result.rejectValue(
                    "nro_documento",
                    "dni.duplicado",
                    "Ya existe un cliente registrado con este DNI"
            );

            // cargar tabla otra vez
            model.addAttribute("clientes", clienteService.listarTodos());

            model.addAttribute("cliente", cliente);
            model.addAttribute("abrirModalCliente", true);

            return "clientes/clientes";

        } catch (Exception e) {
            e.printStackTrace();
            result.reject("errorGeneral", "Ocurrió un error inesperado");

            model.addAttribute("clientes", clienteService.listarTodos());
            model.addAttribute("cliente", cliente);
            model.addAttribute("abrirModalCliente", true);

            return "clientes/clientes";
        }
    }



    // ============================================================
// VISTA EN CARDS (AJAX)
// ============================================================
    @GetMapping("/cards")
    public String vistaCards(Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        return "clientes/cards :: cardsContenido";
    }


}
