package com.dentia.controller.interno;

import com.dentia.dto.interno.clientes.ClienteComprasPaginaDto;
import com.dentia.dto.interno.clientes.ClienteDetalleComercialDto;
import com.dentia.dto.interno.clientes.ClientePaginaDto;
import com.dentia.dto.interno.clientes.ClienteRankingDto;
import com.dentia.model.interno.Cliente;
import com.dentia.service.interno.SedeService;
import com.dentia.service.interno.ClienteService;
import com.dentia.model.shared.ClienteImportUtil;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

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

import java.util.Map;
import java.util.HashMap;




import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Locale;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
@RequestMapping("/interno/clientes")
public class ClienteController {

    private static final Logger log = LoggerFactory.getLogger(ClienteController.class);
    private final ClienteService clienteService;
    private final SedeService sedeService;

    public ClienteController(ClienteService clienteService, SedeService sedeService) {
        this.clienteService = clienteService;
        this.sedeService = sedeService;
    }

    // ============================================================
    // LISTADO + DASHBOARD
    // ============================================================
    @GetMapping
    public String listarClientes(
            Model model,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "TODOS") String filtro,
            @RequestParam(defaultValue = "tabla") String vista) {
        cargarPaginaEnModelo(model, buscar, filtro, pagina, tamano, vista);
        cargarEstadisticas(model);
        model.addAttribute("sedes", sedeService.listarActivas());
        model.addAttribute("aniosCompras", clienteService.listarAniosConCompras());

        return "interno/clientes/clientes";
    }

    // ============================================================
    // BÚSQUEDA AJAX
    // ============================================================
    @GetMapping("/buscar")
    public String buscar(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "TODOS") String filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            @RequestParam(defaultValue = "tabla") String vista,
            Model model) {
        cargarPaginaEnModelo(model, q, filtro, pagina, tamano, vista);
        return "interno/clientes/clientes :: listadoClientes";
    }

    @GetMapping("/pagina")
    public String pagina(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "TODOS") String filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            @RequestParam(defaultValue = "tabla") String vista,
            Model model) {
        cargarPaginaEnModelo(model, buscar, filtro, pagina, tamano, vista);
        return "interno/clientes/clientes :: listadoClientes";
    }

    // ============================================================
    // FILTROS
    // ============================================================
    @GetMapping("/filtro/{tipo}")
    public String filtrar(
            @PathVariable String tipo,
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            @RequestParam(defaultValue = "tabla") String vista,
            Model model) {
        cargarPaginaEnModelo(model, buscar, tipo, pagina, tamano, vista);
        return "interno/clientes/clientes :: listadoClientes";
    }

    @GetMapping("/api/{id}/detalle")
    @ResponseBody
    public ResponseEntity<ClienteDetalleComercialDto> detalleComercial(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(clienteService.obtenerDetalleComercial(id));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/api/{id}/compras")
    @ResponseBody
    public ResponseEntity<ClienteComprasPaginaDto> comprasCliente(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "TODOS") String canal,
            @RequestParam(defaultValue = "TODOS") String estado,
            @RequestParam(required = false) Integer sede,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        try {
            return ResponseEntity.ok(clienteService.obtenerComprasCliente(id, buscar, canal, estado, sede,
                    fecha, mes, anio, desde, hasta, pagina, tamano));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/api/ranking")
    @ResponseBody
    public List<ClienteRankingDto> ranking(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            @RequestParam(defaultValue = "TODOS") String canal,
            @RequestParam(required = false) Integer sede,
            @RequestParam(defaultValue = "MONTO") String criterio,
            @RequestParam(defaultValue = "10") int limite) {
        try {
            return clienteService.obtenerRanking(mes, anio, canal, sede, criterio, limite);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
        }
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> clienteNoEncontrado(NoSuchElementException ex) {
        return ResponseEntity.status(NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // ============================================================
    // PERFIL (MODAL)
    // ============================================================
    @GetMapping("/perfil/{id}")
    public String perfil(@PathVariable Integer id, Model model) {

        Cliente c = clienteService.obtenerPerfilCompleto(id);

        if (c == null) {
            c = clienteService.buscarPorId(id);
        }

        if (c == null) {
            throw new ResponseStatusException(NOT_FOUND, "Cliente no encontrado");
        }

        model.addAttribute("c", c);
        return "interno/clientes/perfil :: perfilContenido";
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
        if (clienteService.buscarPorId(id) == null) throw new ResponseStatusException(NOT_FOUND, "Cliente no encontrado");
        String notaSegura = nota != null ? nota.trim() : "";
        if (notaSegura.isEmpty() || notaSegura.length() > 1000) {
            throw new ResponseStatusException(BAD_REQUEST, "La nota debe contener entre 1 y 1000 caracteres");
        }
        clienteService.agregarNota(id, notaSegura);
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

            if (archivo.getSize() > 5L * 1024 * 1024) {
                return "ERROR: el archivo supera el máximo de 5 MB";
            }

            String original = archivo.getOriginalFilename();
            if (original == null || original.isBlank()) {
                return "ERROR: el archivo no tiene un nombre válido";
            }
            String nombre = original.toLowerCase(Locale.ROOT);
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
            log.warn("No se pudo importar el archivo de clientes: {}", e.getMessage());
            return "Error al importar el archivo. Revisa el formato y vuelve a intentarlo.";
        }
    }
    // ============================================================
// CREAR CLIENTE AUTOMÁTICO PARA VENTAS
// ============================================================
    @PostMapping("/crear-automatico")
    @ResponseBody
    public Map<String, Object> crearAutomatico(@RequestBody Cliente c) {

        // Ver si existe
        Cliente existente = clienteService.buscarPorDocumento(c.getNro_documento());

        if (existente != null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("id", existente.getIdClientes());
            resp.put("nuevo", false);
            return resp;
        }

        // Registrar nuevo
        Cliente nuevo = clienteService.crearAutomatico(c);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", nuevo.getIdClientes());
        resp.put("nuevo", true);

        return resp;
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

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=clientes.csv");

        OutputStream os = response.getOutputStream();

        String header = "ID,Nombres,Apellidos,DNI,Correo,Telefono,VIP,Moroso,Activo\n";
        os.write(header.getBytes(StandardCharsets.UTF_8));

        for (Cliente c : clientes) {
            String linea = String.format(
                    "%d,%s,%s,%s,%s,%s,%d,%d,%d\n",
                    c.getIdClientes(),
                    csv(c.getNombres()),
                    csv(c.getApellidos()),
                    csv(c.getNro_documento()),
                    csv(c.getCorreo()),
                    csv(c.getTelefono()),
                    c.isVip() ? 1 : 0,
                    c.isMoroso() ? 1 : 0,
                    c.isActivo() ? 1 : 0
            );
            os.write(linea.getBytes(StandardCharsets.UTF_8));
        }

        os.flush();
    }

    private String csv(String valor) {
        String seguro = valor == null ? "" : valor;
        if (!seguro.isEmpty() && "=+-@".indexOf(seguro.charAt(0)) >= 0) seguro = "'" + seguro;
        return '"' + seguro.replace("\"", "\"\"") + '"';
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
    public String nuevoCliente(
            @RequestParam(value = "fragmento", required = false) Boolean fragmento,
            Model model
    ) {

        Cliente cliente = new Cliente();
        cliente.setActivo(true);

        model.addAttribute("cliente", cliente);

        return "interno/clientes/form-cliente :: formCliente";
    }

    // ============================================================
// FORMULARIO EDITAR CLIENTE
// ============================================================
    @GetMapping("/editar/{id}")
    public String editarCliente(@PathVariable Integer id,
                                Model model,
                                @RequestParam(value = "fragmento", required = false) Boolean fragmento) {

        Cliente cliente = clienteService.buscarPorId(id);
        if (cliente == null) return "redirect:/interno/clientes?error=NoExiste";

        model.addAttribute("cliente", cliente);

        {
            return "interno/clientes/form-cliente :: formCliente";
        }

    }

    // ============================================================
// GUARDAR CLIENTE (CREAR O ACTUALIZAR)
// ============================================================
    @PostMapping("/guardar")
    public String guardarCliente(
            @ModelAttribute("cliente") Cliente cliente,
            BindingResult result,
            @RequestParam(value = "fileFoto", required = false) MultipartFile archivoFoto,
            @RequestParam(defaultValue = "false") boolean eliminarFoto,
            @RequestParam(defaultValue = "0") int paginaRetorno,
            @RequestParam(defaultValue = "10") int tamanoRetorno,
            @RequestParam(defaultValue = "") String buscarRetorno,
            @RequestParam(defaultValue = "TODOS") String filtroRetorno,
            @RequestParam(defaultValue = "tabla") String vistaRetorno,
            Model model
    ) {

        String documento = cliente.getNro_documento() != null ? cliente.getNro_documento().trim() : "";
        cliente.setNro_documento(documento);
        if (!(documento.matches("\\d{8}") || documento.matches("\\d{11}"))) {
            result.rejectValue("nro_documento", "documento.invalido", "Ingrese un DNI de 8 o RUC de 11 dígitos");
        } else {
            cliente.setIdTipoDocumento(documento.length() == 11 ? 2 : 1);
            Cliente duplicado = clienteService.buscarPorDocumento(documento);
            if (duplicado != null && !duplicado.getIdClientes().equals(cliente.getIdClientes())) {
                result.rejectValue("nro_documento", "documento.duplicado", "Ya existe un cliente con este documento");
            }
        }

        if (documento.length() == 11) {
            if (cliente.getRazonSocial() == null || cliente.getRazonSocial().isBlank()) {
                result.rejectValue("razonSocial", "razon.vacia", "Ingrese la razón social");
            }
        } else {
            if (cliente.getNombres() == null || cliente.getNombres().isBlank()) {
                result.rejectValue("nombres", "nombres.vacio", "Ingrese los nombres");
            }
            if (cliente.getApellidos() == null || cliente.getApellidos().isBlank()) {
                result.rejectValue("apellidos", "apellidos.vacio", "Ingrese los apellidos");
            }
        }

        if (cliente.getCorreo() != null && !cliente.getCorreo().isBlank()
                && !cliente.getCorreo().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            result.rejectValue("correo", "correo.invalido", "Ingrese un correo válido");
        }
        if (cliente.getTelefono() != null && !cliente.getTelefono().isBlank()
                && !cliente.getTelefono().trim().matches("\\d{7,15}")) {
            result.rejectValue("telefono", "telefono.invalido", "El teléfono debe contener entre 7 y 15 dígitos");
        }

        // ❗ SI HAY ERRORES (cualquier tipo)
        if (result.hasErrors()) {

            // cargar lista de clientes nuevamente para renderizar la tabla
            cargarSoporteListado(model);

            model.addAttribute("cliente", cliente);
            model.addAttribute("abrirModalCliente", true);

            return "interno/clientes/clientes";
        }

        try {
            // SUBIR FOTO SOLO SI NO ES CLIENTE GOOGLE
            Cliente clienteBD = null;

            if (cliente.getIdClientes() != null && cliente.getIdClientes() > 0) {

                clienteBD = clienteService.buscarPorId(cliente.getIdClientes());

                if (clienteBD != null) {

                    // conservar foto actual, salvo que el usuario solicite quitarla
                    cliente.setFoto(eliminarFoto ? null : clienteBD.getFoto());

                    // conservar GoogleSub
                    cliente.setGoogleSub(clienteBD.getGoogleSub());
                    cliente.setCuentaOnlineActiva(clienteBD.isCuentaOnlineActiva());
                    cliente.setFechaVinculacionGoogle(clienteBD.getFechaVinculacionGoogle());
                    cliente.setPerfilCompleto(clienteBD.isPerfilCompleto());
                }
            }
            if (archivoFoto != null && !archivoFoto.isEmpty()) {

                if (archivoFoto.getSize() > 3L * 1024 * 1024) {
                    throw new IllegalArgumentException("La foto no puede superar 3 MB");
                }
                String tipoContenido = archivoFoto.getContentType();
                if (tipoContenido == null || !List.of("image/jpeg", "image/png", "image/webp").contains(tipoContenido)) {
                    throw new IllegalArgumentException("La foto debe ser JPG, PNG o WEBP");
                }

                boolean puedeCambiarFoto = true;

                if (clienteBD != null &&
                        clienteBD.getGoogleSub() != null &&
                        !clienteBD.getGoogleSub().isBlank()) {

                    puedeCambiarFoto = false;
                }

                if (puedeCambiarFoto) {

                    String nombreOriginal = archivoFoto.getOriginalFilename() != null
                            ? archivoFoto.getOriginalFilename()
                            : "cliente.jpg";

                    String extension = com.dentia.service.shared.FileStorageService.extensionPermitida(archivoFoto);

                    String nombreArchivo = "CLI_"
                            + System.currentTimeMillis()
                            + "_"
                            + java.util.UUID.randomUUID()
                            + extension;

                    Path carpeta = Paths.get(
                            System.getProperty("user.dir"),
                            "uploads",
                            "clientes"
                    ).toAbsolutePath().normalize();

                    Files.createDirectories(carpeta);

                    Path ruta = carpeta.resolve(nombreArchivo)
                            .toAbsolutePath()
                            .normalize();

                    Files.write(ruta, archivoFoto.getBytes());

                    cliente.setFoto(nombreArchivo);
                }
            }

            // CREAR / ACTUALIZAR
            if (cliente.getIdClientes() == null || cliente.getIdClientes() == 0) {
                clienteService.crear(cliente);
            } else {
                clienteService.actualizar(cliente);
            }

            String retorno = UriComponentsBuilder.fromPath("/interno/clientes")
                    .queryParam("pagina", Math.max(0, paginaRetorno))
                    .queryParam("tamano", tamanoRetorno)
                    .queryParam("buscar", buscarRetorno)
                    .queryParam("filtro", filtroRetorno)
                    .queryParam("vista", vistaRetorno)
                    .queryParam("exito")
                    .build().encode().toUriString();
            return "redirect:" + retorno;

        } catch (org.springframework.dao.DuplicateKeyException ex) {

            result.rejectValue(
                    "nro_documento",
                    "dni.duplicado",
                    "Ya existe un cliente registrado con este DNI"
            );

            // cargar tabla otra vez
            cargarSoporteListado(model);

            model.addAttribute("cliente", cliente);
            model.addAttribute("abrirModalCliente", true);

            return "interno/clientes/clientes";

        } catch (Exception e) {
            log.error("Error inesperado guardando cliente", e);
            result.reject("errorGeneral", "Ocurrió un error inesperado");

            cargarSoporteListado(model);
            model.addAttribute("cliente", cliente);
            model.addAttribute("abrirModalCliente", true);

            return "interno/clientes/clientes";
        }
    }



    // ============================================================
// VISTA EN CARDS (AJAX)
// ============================================================
    @GetMapping("/cards")
    public String vistaCards(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "TODOS") String filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            Model model) {
        ClientePaginaDto paginaClientes = clienteService.obtenerPagina(buscar, filtro, pagina, tamano);
        model.addAttribute("clientes", paginaClientes.clientes());
        model.addAttribute("resumenesClientes", paginaClientes.resumenes());
        return "interno/clientes/cards :: cardsContenido";
    }

    private void cargarSoporteListado(Model model) {
        cargarPaginaEnModelo(model, "", "TODOS", 0, 10, "tabla");
        cargarEstadisticas(model);
        model.addAttribute("sedes", sedeService.listarActivas());
        model.addAttribute("aniosCompras", clienteService.listarAniosConCompras());
    }

    private void cargarPaginaEnModelo(Model model, String buscar, String filtro, int pagina, int tamano, String vista) {
        ClientePaginaDto resultado = clienteService.obtenerPagina(buscar, filtro, pagina, tamano);
        String vistaSegura = "cards".equalsIgnoreCase(vista) ? "cards" : "tabla";
        model.addAttribute("clientes", resultado.clientes());
        model.addAttribute("resumenesClientes", resultado.resumenes());
        model.addAttribute("paginaActual", resultado.pagina());
        model.addAttribute("tamanoPagina", resultado.tamano());
        model.addAttribute("totalElementos", resultado.totalElementos());
        model.addAttribute("totalPaginas", resultado.totalPaginas());
        model.addAttribute("elementoDesde", resultado.desde());
        model.addAttribute("elementoHasta", resultado.hasta());
        model.addAttribute("paginaInicio", Math.max(0, resultado.pagina() - 2));
        model.addAttribute("paginaFin", Math.max(0, Math.min(resultado.totalPaginas() - 1, resultado.pagina() + 2)));
        model.addAttribute("busquedaActual", buscar == null ? "" : buscar.trim());
        model.addAttribute("filtroActual", filtro == null ? "TODOS" : filtro.trim().toUpperCase(Locale.ROOT));
        model.addAttribute("vistaActual", vistaSegura);
    }

    private void cargarEstadisticas(Model model) {
        model.addAttribute("totalClientes", clienteService.contarClientes("TODOS"));
        model.addAttribute("clientesNuevos", clienteService.contarClientes("NUEVO"));
        model.addAttribute("clientesInactivos", clienteService.contarClientes("INACTIVO"));
        model.addAttribute("clientesVip", clienteService.contarClientes("VIP"));
        model.addAttribute("clientesOnline", clienteService.contarClientes("ONLINE"));
        model.addAttribute("clientesPresenciales", clienteService.contarClientes("PRESENCIAL"));
        model.addAttribute("clientesMixtos", clienteService.contarClientes("MIXTO"));
    }


}
