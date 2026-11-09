package com.dentia.controller.interno;

import com.dentia.model.interno.Producto;
import com.dentia.model.interno.ProductoFoto;
import com.dentia.model.interno.ProductoPresentacionOferta;
import com.dentia.service.interno.ProductoCatalogoService;
import com.dentia.service.interno.ProductoFotoService;
import com.dentia.service.interno.ProductoPresentacionOfertaService;
import com.dentia.service.interno.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.dentia.model.interno.ProductoPrecioPresentacion;
import com.dentia.service.interno.ProductoPrecioPresentacionService;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/interno/productos")
public class ProductoController {

    private final ProductoService service;
    private final ProductoCatalogoService catalogoService;
    private final ProductoPrecioPresentacionService precioPresentacionService;
    private final ProductoFotoService productoFotoService;
    private final ProductoPresentacionOfertaService productoPresentacionOfertaService;

    public ProductoController(ProductoService service,
                               ProductoCatalogoService catalogoService,
                               ProductoPrecioPresentacionService precioPresentacionService,
                               ProductoFotoService productoFotoService,
                               ProductoPresentacionOfertaService productoPresentacionOfertaService) {
        this.service = service;
        this.catalogoService = catalogoService;
        this.precioPresentacionService = precioPresentacionService;
        this.productoFotoService = productoFotoService;
        this.productoPresentacionOfertaService = productoPresentacionOfertaService;
    }

    // ============================================================
    // LISTAR PANTALLA PRINCIPAL
    // ============================================================

    @GetMapping
    public String listar(@RequestParam(value = "buscar", required = false) String filtro,
                         @RequestParam(value = "estado", required = false) String estado,
                         Model model) {

        model.addAttribute("titulo", "Gestión de Productos | DentIA");

        List<Producto> productos;

        if ("eliminados".equalsIgnoreCase(estado)) {
            productos = service.listarEliminados();
        } else {
            productos = (filtro != null && !filtro.isBlank())
                    ? service.buscar(filtro.trim())
                    : service.listar();
        }

        model.addAttribute("productos", productos);

        long totalProductos = productos.size();
        long productosActivos = productos.stream()
                .filter(p -> p.getIdEstadoProducto() != null && p.getIdEstadoProducto() == 1)
                .count();

        long productosInactivos = productos.stream()
                .filter(p -> p.getIdEstadoProducto() != null && p.getIdEstadoProducto() == 2)
                .count();

        long productosEliminados = productos.stream()
                .filter(p -> p.getIdEstadoProducto() != null && p.getIdEstadoProducto() == 3)
                .count();

        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("productosActivos", productosActivos);
        model.addAttribute("productosInactivos", productosInactivos);
        model.addAttribute("productosEliminados", productosEliminados);

        if (!model.containsAttribute("productoForm")) {
            model.addAttribute("productoForm", new Producto());
        }

        model.addAttribute("filtro", filtro);
        model.addAttribute("estadoFiltro", estado);

        cargarCatalogos(model);

        return "interno/productos/list";
    }

    private Integer obtenerSedeActivaId(HttpSession session) {

        if (session == null) {
            return null;
        }

        Object sedeObj = session.getAttribute("sedeActivaId");

        if (sedeObj instanceof Integer id) {
            return id;
        }

        if (sedeObj instanceof String sedeStr) {
            try {
                return Integer.parseInt(sedeStr);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private void cargarCatalogos(Model model) {

        // Para mantenedores: mostrar activos e inactivos
        model.addAttribute("marcas", catalogoService.listarMarcas());
        model.addAttribute("categorias", catalogoService.listarCategorias());
        model.addAttribute("subcategorias", catalogoService.listarSubcategorias());
        model.addAttribute("unidades", catalogoService.listarUnidades());
        model.addAttribute("atributos", catalogoService.listarAtributos());

        // Para crear/editar producto: mostrar solo activos
        model.addAttribute("marcasActivas", catalogoService.listarMarcasActivas());
        model.addAttribute("categoriasActivas", catalogoService.listarCategoriasActivas());
        model.addAttribute("subcategoriasActivas", catalogoService.listarSubcategoriasActivas());
        model.addAttribute("unidadesActivas", catalogoService.listarUnidadesActivas());
    }

    // ============================================================
    // GUARDAR PRODUCTO
    // ============================================================

    @PostMapping
    public String guardar(
            @Valid @ModelAttribute("productoForm") Producto form,
            BindingResult br,
            @RequestParam Map<String, String> params,
            @RequestParam(value = "fotoArchivo", required = false) MultipartFile imagen,
            @RequestParam(value = "fotosProducto", required = false) MultipartFile[] fotosProducto,
            @RequestParam(value = "fotoPortadaTemporal", required = false) Integer fotoPortadaTemporal,
            RedirectAttributes redirectAttrs
    ) {

        if (br.hasErrors()) {
            redirectAttrs.addFlashAttribute("productoForm", form);
            redirectAttrs.addFlashAttribute("modalError", true);
            redirectAttrs.addFlashAttribute("mensaje", "Hay errores en el formulario. Revisa los campos obligatorios.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/interno/productos";
        }

        try {

            if (form.getDescuentoMaximo() == null) {
                form.setDescuentoMaximo(BigDecimal.ZERO);
            }

            if (form.getPrecioUnitario() == null) {
                form.setPrecioUnitario(BigDecimal.ZERO);
            }

            if (form.getDescuentoMaximo().compareTo(form.getPrecioUnitario()) > 0) {
                redirectAttrs.addFlashAttribute("productoForm", form);
                redirectAttrs.addFlashAttribute("modalError", true);
                redirectAttrs.addFlashAttribute("mensaje", "El descuento máximo no puede ser mayor al precio principal.");
                redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
                return "redirect:/interno/productos";
            }

            // Si está editando y no sube nueva foto, conservar la anterior
            if ((imagen == null || imagen.isEmpty()) && form.getIdProducto() != null) {
                Producto existente = service.obtener(form.getIdProducto());

                if (existente != null) {
                    form.setFoto(existente.getFoto());
                }
            }

            // Si sube nueva foto, guardarla en la carpeta raíz /uploads/productos
            if (imagen != null && !imagen.isEmpty()) {

                String contentType = imagen.getContentType();

                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new RuntimeException("El archivo del producto debe ser una imagen.");
                }

                String extension = com.dentia.service.shared.FileStorageService.extensionPermitida(imagen);

                String nombreArchivo = "PROD_"
                        + System.currentTimeMillis()
                        + "_"
                        + java.util.UUID.randomUUID()
                        + extension;

                Path carpeta = Paths.get(System.getProperty("user.dir"), "uploads", "productos")
                        .toAbsolutePath()
                        .normalize();

                Files.createDirectories(carpeta);

                Path ruta = carpeta.resolve(nombreArchivo)
                        .toAbsolutePath()
                        .normalize();

                Files.copy(
                        imagen.getInputStream(),
                        ruta,
                        StandardCopyOption.REPLACE_EXISTING
                );

                form.setFoto(nombreArchivo);
            }

            Map<Integer, String> atributosProducto = new HashMap<>();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key != null && key.startsWith("atributo_")) {
                    String idTexto = key.replace("atributo_", "");

                    try {
                        Integer idAtributo = Integer.parseInt(idTexto);

                        if (value != null && !value.trim().isBlank()) {
                            atributosProducto.put(idAtributo, value.trim());
                        }

                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            Integer estadoDuplicado =
                    catalogoService.obtenerEstadoDuplicadoConAtributos(form, atributosProducto);

            if (estadoDuplicado != null) {

                redirectAttrs.addFlashAttribute("productoForm", form);
                redirectAttrs.addFlashAttribute("modalError", true);
                redirectAttrs.addFlashAttribute("tipoMensaje", "danger");

                if (estadoDuplicado == 3) {
                    redirectAttrs.addFlashAttribute(
                            "mensaje",
                            "Este producto ya existe con los mismos atributos técnicos, pero está en eliminados. Restáuralo para volver a usarlo."
                    );
                } else if (estadoDuplicado == 2) {
                    redirectAttrs.addFlashAttribute(
                            "mensaje",
                            "Ya existe un producto inactivo con los mismos datos y atributos técnicos. Actívalo para volver a usarlo."
                    );
                } else {
                    redirectAttrs.addFlashAttribute(
                            "mensaje",
                            "Ya existe un producto activo con los mismos datos y atributos técnicos."
                    );
                }

                return "redirect:/interno/productos";
            }

            Integer idProductoGuardado = service.guardarYRetornarId(form);

            catalogoService.guardarAtributosDeProducto(idProductoGuardado, atributosProducto);

            guardarFotosProductoNuevo(idProductoGuardado, fotosProducto, fotoPortadaTemporal);

            redirectAttrs.addFlashAttribute("mensaje", "Producto guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

            return "redirect:/interno/productos";

        } catch (Exception ex) {

            BigDecimal ultimo = null;

            if (form.getIdProducto() != null) {
                ultimo = service.obtenerUltimoPrecioCompra(form.getIdProducto());
            }

            redirectAttrs.addFlashAttribute("productoForm", form);
            redirectAttrs.addFlashAttribute("modalError", true);
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
            redirectAttrs.addFlashAttribute("errorPrecioVenta", ex.getMessage());
            redirectAttrs.addFlashAttribute("ultimoPrecioCompraError", ultimo);

            return "redirect:/interno/productos";
        }
    }

    private void guardarFotosProductoNuevo(
            Integer idProducto,
            MultipartFile[] fotosProducto,
            Integer fotoPortadaTemporal) {

        if (fotosProducto == null || fotosProducto.length == 0) {
            return;
        }

        List<MultipartFile> fotosValidas = new ArrayList<>();

        for (MultipartFile foto : fotosProducto) {
            if (foto == null || foto.isEmpty()) {
                continue;
            }

            fotosValidas.add(foto);
        }

        if (fotosValidas.isEmpty()) {
            return;
        }

        int fotosExistentes = productoFotoService.listarActivas(idProducto).size();
        int disponibles = 3 - fotosExistentes;

        if (fotosValidas.size() > disponibles) {
            throw new IllegalArgumentException("Maximo 3 imagenes por producto.");
        }

        List<ProductoFoto> fotosGuardadas = new ArrayList<>();

        for (MultipartFile foto : fotosValidas) {
            fotosGuardadas.add(productoFotoService.subirFoto(idProducto, foto));
        }

        if (fotosGuardadas.isEmpty()) {
            return;
        }

        int indicePortada = fotoPortadaTemporal != null ? fotoPortadaTemporal : -1;

        if (indicePortada < 0) {
            return;
        }

        if (indicePortada >= fotosGuardadas.size()) {
            indicePortada = 0;
        }

        ProductoFoto portada = fotosGuardadas.get(indicePortada);
        productoFotoService.marcarPortada(portada.getIdProductoFoto());
    }

    // ============================================================
    // API: OBTENER PRODUCTO POR ID
    // ============================================================

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiObtener(
            @PathVariable Integer id,
            HttpSession session) {

        Producto producto = service.obtener(id);

        if (producto == null) {
            return ResponseEntity.notFound().build();
        }

        Object sedeObj = session.getAttribute("sedeActivaId");

        Integer sedeActivaId = null;

        if (sedeObj instanceof Integer sedeId) {
            sedeActivaId = sedeId;
        } else if (sedeObj instanceof String sedeStr) {
            try {
                sedeActivaId = Integer.parseInt(sedeStr);
            } catch (NumberFormatException ignored) {
                sedeActivaId = null;
            }
        }

        BigDecimal ultimoPrecioCompra = service.obtenerUltimoPrecioCompra(id);

        BigDecimal costoPromedioSedeActiva = BigDecimal.ZERO;

        if (sedeActivaId != null) {
            costoPromedioSedeActiva =
                    service.obtenerCostoPromedioPorSede(id, sedeActivaId);
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("producto", producto);
        respuesta.put("ultimoPrecioCompra", ultimoPrecioCompra);
        respuesta.put("costoPromedioSedeActiva", costoPromedioSedeActiva);
        respuesta.put("idSedeActiva", sedeActivaId);

        // Para editar producto: necesita idAtributo -> valor
        respuesta.put("atributosValores", catalogoService.listarValoresAtributosPorProducto(id));

        // Para ver detalle: necesita nombreAtributo -> valor
        respuesta.put("atributosDetalle", catalogoService.listarValoresAtributosDetallePorProducto(id));

        return ResponseEntity.ok(respuesta);
    }

    // ============================================================
// API: PRECIOS / PRESENTACIONES DE VENTA POR PRODUCTO
// ============================================================

    @GetMapping("/{idProducto}/precios")
    @ResponseBody
    public List<ProductoPrecioPresentacion> listarPreciosProducto(
            @PathVariable Integer idProducto,
            HttpSession session) {

        return precioPresentacionService.listarPorProducto(idProducto);
    }

    @GetMapping("/{idProducto}/presentaciones-venta")
    @ResponseBody
    public List<Map<String, Object>> listarPresentacionesVenta(
            @PathVariable Integer idProducto,
            HttpSession session) {

        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return List.of();
        }

        Producto producto = service.obtener(idProducto);

        if (producto == null) {
            return List.of();
        }

        List<ProductoPrecioPresentacion> lista =
                precioPresentacionService
                        .listarActivasPorProductoYSede(
                                idProducto,
                                sedeActivaId
                        );

        List<Map<String, Object>> respuesta = new ArrayList<>();

        String tipoMedidaProducto =
                producto.getTipoMedidaBase() == null
                        ? ""
                        : producto.getTipoMedidaBase()
                        .trim()
                        .toUpperCase();

        for (ProductoPrecioPresentacion p : lista) {

            Map<String, Object> item = new HashMap<>();

            String nombrePresentacion =
                    p.getNombrePresentacion() == null
                            ? ""
                            : p.getNombrePresentacion()
                            .trim()
                            .toUpperCase();

            String tipoCalculoVenta =
                    p.getTipoCalculoVenta() == null
                            ? ""
                            : p.getTipoCalculoVenta()
                            .trim()
                            .toUpperCase();

            boolean presentacionCompleta =
                    nombrePresentacion.contains("PLANCHA COMPLETA")
                            || nombrePresentacion.equals("PLANCHA")
                            || nombrePresentacion.contains("VARILLA COMPLETA")
                            || nombrePresentacion.equals("VARILLA")
                            || nombrePresentacion.contains("ROLLO COMPLETO")
                            || tipoCalculoVenta.equals("COMPLETA");

            boolean presentacionSuelta =
                    tipoCalculoVenta.equals("PIE2")
                            || tipoCalculoVenta.equals("M2")
                            || tipoCalculoVenta.equals("METRO_CUADRADO")
                            || tipoCalculoVenta.equals("METRO_LINEAL")
                            || tipoCalculoVenta.equals("LONGITUD");

            BigDecimal largoBase = p.getLargoBase();
            BigDecimal anchoBase = p.getAnchoBase();
            BigDecimal medidaTotalBase = p.getMedidaTotalBase();

            /*
             * Solo la presentación física completa puede usar como respaldo
             * las dimensiones generales del producto.
             *
             * Media plancha, cuarto de plancha y otras presentaciones físicas
             * deben tener sus propias medidas registradas.
             */
            if (presentacionCompleta) {

                if (largoBase == null
                        || largoBase.compareTo(BigDecimal.ZERO) <= 0) {
                    largoBase = producto.getLargoBase();
                }

                if (anchoBase == null
                        || anchoBase.compareTo(BigDecimal.ZERO) <= 0) {
                    anchoBase = producto.getAnchoBase();
                }

                if (medidaTotalBase == null
                        || medidaTotalBase.compareTo(BigDecimal.ZERO) <= 0) {
                    medidaTotalBase = producto.getMedidaTotalBase();
                }
            }

            /*
             * Las presentaciones sueltas se cobran por unidad comercial,
             * pero las dimensiones generales del producto solo se envían
             * como referencia física para la interfaz.
             *
             * No se convierten en dimensiones propias de la presentación.
             */
            if (presentacionSuelta) {

                if ("AREA".equals(tipoMedidaProducto)) {

                    if (largoBase == null
                            || largoBase.compareTo(BigDecimal.ZERO) <= 0) {
                        largoBase = producto.getLargoBase();
                    }

                    if (anchoBase == null
                            || anchoBase.compareTo(BigDecimal.ZERO) <= 0) {
                        anchoBase = producto.getAnchoBase();
                    }

                } else if ("LONGITUD".equals(tipoMedidaProducto)) {

                    if (medidaTotalBase == null
                            || medidaTotalBase.compareTo(BigDecimal.ZERO) <= 0) {
                        medidaTotalBase = producto.getMedidaTotalBase();
                    }
                }
            }

            boolean productoTieneMedida;

            if ("AREA".equals(tipoMedidaProducto)) {

                productoTieneMedida =
                        largoBase != null
                                && largoBase.compareTo(BigDecimal.ZERO) > 0
                                && anchoBase != null
                                && anchoBase.compareTo(BigDecimal.ZERO) > 0;

                /*
                 * En base de datos las dimensiones están en metros.
                 * ventas.js trabaja estas dimensiones en centímetros.
                 */
                if (productoTieneMedida) {
                    largoBase = largoBase
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);

                    anchoBase = anchoBase
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);
                }

                /*
                 * Para la interfaz antigua, medidaTotalBase de área se expresa
                 * en pie². La medida física real sigue determinada por largo/ancho.
                 */
                if (medidaTotalBase != null
                        && medidaTotalBase.compareTo(BigDecimal.ZERO) > 0) {

                    medidaTotalBase = medidaTotalBase
                            .multiply(new BigDecimal("10.7639104167"))
                            .setScale(4, RoundingMode.HALF_UP);
                }

                if (tipoCalculoVenta.equals("PIE2")) {
                    medidaTotalBase = BigDecimal.ONE;
                }

                if (tipoCalculoVenta.equals("M2")
                        || tipoCalculoVenta.equals("METRO_CUADRADO")) {

                    medidaTotalBase =
                            new BigDecimal("10.7639104167")
                                    .setScale(4, RoundingMode.HALF_UP);
                }

            } else if ("LONGITUD".equals(tipoMedidaProducto)) {

                productoTieneMedida =
                        medidaTotalBase != null
                                && medidaTotalBase.compareTo(BigDecimal.ZERO) > 0;

                /*
                 * Para el flujo anterior de edición, la longitud base se
                 * mantiene expresada en metros.
                 */
                if (tipoCalculoVenta.equals("METRO_LINEAL")) {
                    medidaTotalBase = BigDecimal.ONE;
                }

                largoBase = null;
                anchoBase = null;

            } else {
                productoTieneMedida = false;
            }

            item.put(
                    "idPrecioPresentacion",
                    p.getIdPrecioPresentacion()
            );

            item.put(
                    "idProducto",
                    p.getIdProducto()
            );

            item.put(
                    "idSede",
                    p.getIdSede()
            );

            item.put(
                    "nombrePresentacion",
                    p.getNombrePresentacion()
            );

            item.put(
                    "idUnidadVenta",
                    p.getIdUnidadVenta()
            );

            item.put(
                    "nombreUnidadVenta",
                    p.getNombreUnidadVenta()
            );

            item.put(
                    "abreviaturaUnidadVenta",
                    p.getAbreviaturaUnidadVenta()
            );

            /*
             * Se conserva el tipo comercial real:
             * PIE2, M2, METRO_LINEAL, CANTIDAD, etc.
             */
            item.put(
                    "tipoCalculoVenta",
                    p.getTipoCalculoVenta()
            );

            item.put(
                    "tipoCalculoVentaOriginal",
                    p.getTipoCalculoVenta()
            );

            item.put(
                    "cantidadIncluida",
                    p.getCantidadIncluida()
            );

            item.put(
                    "factorConversionStock",
                    p.getFactorConversionStock()
            );

            item.put(
                    "productoTieneMedida",
                    productoTieneMedida
            );

            item.put(
                    "tipoMedidaProducto",
                    tipoMedidaProducto
            );

            item.put(
                    "largoBase",
                    largoBase
            );

            item.put(
                    "anchoBase",
                    anchoBase
            );

            item.put(
                    "medidaTotalBase",
                    medidaTotalBase
            );

            item.put(
                    "precioVenta",
                    p.getPrecioVenta()
            );

            item.put(
                    "descuentoMaximo",
                    p.getDescuentoMaximo()
            );

            item.put(
                    "descripcionUso",
                    p.getDescripcionUso()
            );

            respuesta.add(item);
        }

        return respuesta;
    }

    @PostMapping("/{idProducto}/precios")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarPrecioProducto(
            @PathVariable Integer idProducto,
            @RequestBody ProductoPrecioPresentacion form,
            HttpSession session) {

        try {
            form.setIdProducto(idProducto);

            precioPresentacionService.guardar(form);

            Map<String, Object> body = new HashMap<>();
            body.put("ok", true);
            body.put("mensaje", "Presentacion guardada correctamente.");

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("mensaje", ex.getMessage());

            return ResponseEntity.badRequest().body(body);
        }
    }

    @PostMapping("/precios/{idPrecioPresentacion}/estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstadoPrecioProducto(
            @PathVariable Integer idPrecioPresentacion,
            @RequestParam("estado") Integer estado) {

        try {
            precioPresentacionService.cambiarEstado(idPrecioPresentacion, estado);

            Map<String, Object> body = new HashMap<>();
            body.put("ok", true);
            body.put("mensaje", "Estado actualizado correctamente.");

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("mensaje", ex.getMessage());

            return ResponseEntity.badRequest().body(body);
        }
    }

    @PostMapping("/precios/{idPrecioPresentacion}/portada")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcarPrecioPortada(
            @PathVariable Integer idPrecioPresentacion) {

        try {
            precioPresentacionService.marcarPrecioPortada(idPrecioPresentacion);

            Map<String, Object> body = new HashMap<>();
            body.put("ok", true);
            body.put("mensaje", "Precio portada actualizado correctamente.");

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("mensaje", ex.getMessage());

            return ResponseEntity.badRequest().body(body);
        }
    }

    @GetMapping("/presentaciones/{idPrecioPresentacion}/oferta")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerOfertaPresentacion(
            @PathVariable Integer idPrecioPresentacion) {

        try {
            ProductoPresentacionOferta oferta = productoPresentacionOfertaService
                    .obtenerActiva(idPrecioPresentacion)
                    .orElse(null);

            Map<String, Object> body = new HashMap<>();
            body.put("ok", true);
            body.put("mensaje", oferta == null ? "La presentacion no tiene oferta activa." : "Oferta activa encontrada.");
            body.put("oferta", oferta);

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("mensaje", ex.getMessage());
            body.put("oferta", null);

            return ResponseEntity.badRequest().body(body);
        }
    }

    @PostMapping("/presentaciones/{idPrecioPresentacion}/oferta")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarOfertaPresentacion(
            @PathVariable Integer idPrecioPresentacion,
            @RequestParam("precioOferta") BigDecimal precioOferta,
            @RequestParam("fechaInicio") String fechaInicio,
            @RequestParam("fechaFin") String fechaFin,
            @RequestParam(value = "textoOferta", required = false) String textoOferta,
            @RequestParam(value = "activo", required = false, defaultValue = "true") Boolean activo) {

        try {
            ProductoPresentacionOferta oferta = productoPresentacionOfertaService.guardarOferta(
                    idPrecioPresentacion,
                    precioOferta,
                    LocalDate.parse(fechaInicio),
                    LocalDate.parse(fechaFin),
                    textoOferta,
                    activo
            );

            Map<String, Object> body = new HashMap<>();
            body.put("ok", true);
            body.put("mensaje", Boolean.TRUE.equals(oferta.getActivo())
                    ? "Oferta guardada correctamente."
                    : "Oferta guardada como inactiva.");
            body.put("oferta", oferta);

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("mensaje", ex.getMessage());
            body.put("oferta", null);

            return ResponseEntity.badRequest().body(body);
        }
    }

    @PostMapping("/presentaciones/{idPrecioPresentacion}/oferta/desactivar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> desactivarOfertaPresentacion(
            @PathVariable Integer idPrecioPresentacion) {

        try {
            productoPresentacionOfertaService.desactivarOferta(idPrecioPresentacion);

            Map<String, Object> body = new HashMap<>();
            body.put("ok", true);
            body.put("mensaje", "Oferta desactivada correctamente.");
            body.put("oferta", null);

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("mensaje", ex.getMessage());
            body.put("oferta", null);

            return ResponseEntity.badRequest().body(body);
        }
    }

    @GetMapping("/{idProducto}/fotos")
    @ResponseBody
    public List<ProductoFoto> listarFotosProducto(@PathVariable Integer idProducto) {
        return productoFotoService.listarActivas(idProducto);
    }

    @PostMapping("/{idProducto}/fotos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> subirFotoProducto(
            @PathVariable Integer idProducto,
            @RequestParam("foto") MultipartFile foto) {

        try {
            ProductoFoto guardada = productoFotoService.subirFoto(idProducto, foto);

            Map<String, Object> body = new HashMap<>();
            body.put("ok", true);
            body.put("mensaje", "Imagen guardada correctamente.");
            body.put("foto", guardada);
            body.put("fotos", productoFotoService.listarActivas(idProducto));

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("mensaje", ex.getMessage());

            return ResponseEntity.badRequest().body(body);
        }
    }

    @PostMapping("/fotos/{idProductoFoto}/portada")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcarFotoPortada(@PathVariable Integer idProductoFoto) {

        try {
            List<ProductoFoto> fotos = productoFotoService.marcarPortada(idProductoFoto);

            Map<String, Object> body = new HashMap<>();
            body.put("ok", true);
            body.put("mensaje", "Portada actualizada correctamente.");
            body.put("fotos", fotos);

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("mensaje", ex.getMessage());

            return ResponseEntity.badRequest().body(body);
        }
    }

    @PostMapping("/fotos/{idProductoFoto}/eliminar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarFotoProducto(@PathVariable Integer idProductoFoto) {

        try {
            productoFotoService.eliminarFoto(idProductoFoto);

            Map<String, Object> body = new HashMap<>();
            body.put("ok", true);
            body.put("mensaje", "Imagen eliminada correctamente.");

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("ok", false);
            body.put("mensaje", ex.getMessage());

            return ResponseEntity.badRequest().body(body);
        }
    }



    // ============================================================
    // ELIMINAR PRODUCTO
    // ============================================================

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Integer id,
                           RedirectAttributes redirectAttrs) {

        Producto producto = service.obtener(id);

        if (producto == null) {
            redirectAttrs.addFlashAttribute("mensaje", "El producto no existe.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "warning");
            return "redirect:/interno/productos";
        }

        try {
            service.eliminar(id);

            redirectAttrs.addFlashAttribute("mensaje", "Producto eliminado correctamente.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", "No se pudo eliminar el producto.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }

    @PostMapping("/{id}/restaurar")
    public String restaurar(@PathVariable Integer id,
                            RedirectAttributes redirectAttrs) {

        try {
            boolean ok = service.restaurar(id);

            if (ok) {
                redirectAttrs.addFlashAttribute("mensaje", "Producto restaurado correctamente.");
                redirectAttrs.addFlashAttribute("tipoMensaje", "success");
            } else {
                redirectAttrs.addFlashAttribute("mensaje", "No se pudo restaurar el producto.");
                redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
            }

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos?estado=eliminados";
    }

    // ============================================================
    // API: GENERAR CÓDIGO INTERNO
    // ============================================================

    @GetMapping("/api/codigo")
    @ResponseBody
    public ResponseEntity<String> apiCodigo(@RequestParam("categoria") Integer idCategoria) {

        if (idCategoria == null || idCategoria <= 0) {
            return ResponseEntity.badRequest().body("Categoría inválida");
        }

        return ResponseEntity.ok(service.generarCodigoInterno(idCategoria));
    }

    // ============================================================
    // API: CAMBIAR ESTADO
    // ============================================================

    @PostMapping("/{id}/estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(
            @PathVariable("id") Integer id,
            @RequestParam("estado") Integer estado) {

        try {
            boolean ok = service.cambiarEstado(id, estado);

            if (!ok) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> body = new HashMap<>();
            body.put("id", id);
            body.put("estado", estado);
            body.put("estadoTexto", estado == 1 ? "Activo" : "Inactivo");

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ============================================================
    // AUTOCOMPLETADO PRODUCTOS
    // Se mantiene para ventas/compras/select2.
    // ============================================================

    @GetMapping("/buscar")
    @ResponseBody
    public List<Map<String, Object>> buscarProductosAjax(
            @RequestParam(value = "term", required = false) String term) {

        String filtro = term == null ? "" : term.trim();

        List<Producto> lista = service.buscar(filtro);
        List<Map<String, Object>> respuesta = new ArrayList<>();

        for (Producto p : lista) {

            Map<String, Object> item = new HashMap<>();

            item.put("id", p.getIdProducto());
            item.put("text", p.getNombreProducto());
            item.put("precio", p.getPrecioUnitario());
            item.put("stock", p.getStockActual());
            item.put("img", p.getFoto());
            item.put("codigo", p.getCodigoInterno());

            item.put("idUnidad", p.getIdUnidad());

            /*
             * NUEVO FLUJO:
             * El producto ya no depende principalmente de la tabla unidades.
             * Ahora se usa:
             * - controlInventario
             * - nombreUnidadInventario
             * - formaVenta
             * - nombreUnidadVenta
             * - abreviaturaUnidadVenta
             */

            item.put("controlInventario", p.getControlInventario());
            item.put("nombreUnidadInventario", p.getNombreUnidadInventario());
            item.put("formaVenta", p.getFormaVenta());
            item.put("nombreUnidadVenta", p.getNombreUnidadVenta());
            item.put("abreviaturaUnidadVenta", p.getAbreviaturaUnidadVenta());

            /*
             * COMPATIBILIDAD TEMPORAL:
             * Algunos módulos antiguos todavía leen unidadNombre,
             * abreviaturaUnidad y tipoCalculoUnidad.
             * Por eso los rellenamos usando el nuevo flujo.
             */

            String unidadNombre = p.getNombreUnidadInventario();

            if (unidadNombre == null || unidadNombre.isBlank()) {
                unidadNombre = p.getNombreUnidad();

                if (unidadNombre == null || unidadNombre.isBlank()) {
                    unidadNombre = "Unidad";
                }
            }

            item.put("unidadNombre", unidadNombre);

            String abreviaturaUnidad = p.getAbreviaturaUnidadVenta();

            if (abreviaturaUnidad == null || abreviaturaUnidad.isBlank()) {
                abreviaturaUnidad = p.getAbreviaturaUnidad();

                if (abreviaturaUnidad == null || abreviaturaUnidad.isBlank()) {
                    abreviaturaUnidad = "und";
                }
            }

            item.put("abreviaturaUnidad", abreviaturaUnidad);

            String tipoCalculoUnidad = p.getFormaVenta();

            if (tipoCalculoUnidad == null || tipoCalculoUnidad.isBlank()) {
                tipoCalculoUnidad = p.getTipoCalculoUnidad();

                if (tipoCalculoUnidad == null || tipoCalculoUnidad.isBlank()) {
                    tipoCalculoUnidad = "CANTIDAD";
                }
            }

            item.put("tipoCalculoUnidad", tipoCalculoUnidad);

            item.put("unidadNombre", unidadNombre);
            item.put("abreviaturaUnidad", p.getAbreviaturaUnidad());
            item.put("tipoCalculoUnidad", p.getTipoCalculoUnidad());

            item.put("idMarca", p.getIdMarca());
            item.put("marca", p.getNombreMarca());

            item.put("idCategoria", p.getIdCategoria());
            item.put("categoria", p.getNombreCategoria());

            item.put("idSubcategoria", p.getIdSubcategoria());
            item.put("subcategoria", p.getNombreSubcategoria());

            item.put("descuentoMaximo", p.getDescuentoMaximo());

            item.put("controlInventario", p.getControlInventario());
            item.put("nombreUnidadInventario", p.getNombreUnidadInventario());
            item.put("formaVenta", p.getFormaVenta());
            item.put("nombreUnidadVenta", p.getNombreUnidadVenta());
            item.put("abreviaturaUnidadVenta", p.getAbreviaturaUnidadVenta());

            item.put("ventaPorMedida", p.getVentaPorMedida());
            item.put("tipoMedidaBase", p.getTipoMedidaBase());
            item.put("largoBase", p.getLargoBase());
            item.put("anchoBase", p.getAnchoBase());
            item.put("medidaTotalBase", p.getMedidaTotalBase());
            item.put("medidaVendibleBase", p.getMedidaVendibleBase());
            item.put("descripcionMedidaBase", p.getDescripcionMedidaBase());

            respuesta.add(item);
        }

        return respuesta;
    }

    @GetMapping("/buscar-vendibles")
    @ResponseBody
    public List<Map<String, Object>> buscarProductosVendiblesAjax(
            @RequestParam(value = "term", required = false) String term,
            HttpSession session) {

        String filtro = term == null ? "" : term.trim();

        Object sedeObj = session.getAttribute("sedeActivaId");

        Integer sedeActivaId = null;

        if (sedeObj instanceof Integer id) {
            sedeActivaId = id;
        } else if (sedeObj instanceof String sedeStr) {
            try {
                sedeActivaId = Integer.parseInt(sedeStr);
            } catch (NumberFormatException e) {
                sedeActivaId = null;
            }
        }

        if (sedeActivaId == null) {
            return List.of();
        }

        List<Producto> lista = service.buscarVendiblesPorSede(filtro, sedeActivaId);
        List<Map<String, Object>> respuesta = new ArrayList<>();

        for (Producto p : lista) {

            BigDecimal precio = p.getPrecioUnitario() == null
                    ? BigDecimal.ZERO
                    : p.getPrecioUnitario();

            BigDecimal stock = p.getStockActualDecimal() == null
                    ? BigDecimal.ZERO
                    : p.getStockActualDecimal();



            if (stock.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();

            item.put("id", p.getIdProducto());
            item.put("text", p.getNombreProducto());
            item.put("precio", BigDecimal.ZERO);
            item.put("stock", stock);
            item.put("img", p.getFoto());
            item.put("codigo", p.getCodigoInterno());

            item.put("idUnidad", p.getIdUnidad());

            String unidadNombre = p.getNombreUnidadInventario();

            if (unidadNombre == null || unidadNombre.isBlank()) {
                unidadNombre = p.getNombreUnidad();

                if (unidadNombre == null || unidadNombre.isBlank()) {
                    unidadNombre = "Unidad";
                }
            }

            item.put("unidadNombre", unidadNombre);

            String abreviaturaUnidad = p.getAbreviaturaUnidadVenta();

            if (abreviaturaUnidad == null || abreviaturaUnidad.isBlank()) {
                abreviaturaUnidad = p.getAbreviaturaUnidad();

                if (abreviaturaUnidad == null || abreviaturaUnidad.isBlank()) {
                    abreviaturaUnidad = "und";
                }
            }

            item.put("abreviaturaUnidad", abreviaturaUnidad);

            String tipoCalculoUnidad = p.getFormaVenta();

            if (tipoCalculoUnidad == null || tipoCalculoUnidad.isBlank()) {
                tipoCalculoUnidad = p.getTipoCalculoUnidad();

                if (tipoCalculoUnidad == null || tipoCalculoUnidad.isBlank()) {
                    tipoCalculoUnidad = "CANTIDAD";
                }
            }

            item.put("tipoCalculoUnidad", tipoCalculoUnidad);

            item.put("idMarca", p.getIdMarca());
            item.put("marca", p.getNombreMarca());

            item.put("idCategoria", p.getIdCategoria());
            item.put("categoria", p.getNombreCategoria());

            item.put("idSubcategoria", p.getIdSubcategoria());
            item.put("subcategoria", p.getNombreSubcategoria());

            item.put("descuentoMaximo", p.getDescuentoMaximo());

            item.put("controlInventario", p.getControlInventario());
            item.put("nombreUnidadInventario", p.getNombreUnidadInventario());
            item.put("formaVenta", p.getFormaVenta());
            item.put("nombreUnidadVenta", p.getNombreUnidadVenta());
            item.put("abreviaturaUnidadVenta", p.getAbreviaturaUnidadVenta());

            item.put("ventaPorMedida", p.getVentaPorMedida());
            item.put("tipoMedidaBase", p.getTipoMedidaBase());
            item.put("largoBase", p.getLargoBase());
            item.put("anchoBase", p.getAnchoBase());
            item.put("medidaTotalBase", p.getMedidaTotalBase());
            item.put("medidaVendibleBase", p.getMedidaVendibleBase());
            item.put("descripcionMedidaBase", p.getDescripcionMedidaBase());

            respuesta.add(item);
        }

        return respuesta;
    }

    @GetMapping("/{idProducto}/stock-sede-activa")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerStockSedeActiva(
            @PathVariable Integer idProducto,
            HttpSession session) {

        Object sedeObj = session.getAttribute("sedeActivaId");

        Integer sedeActivaId = null;

        if (sedeObj instanceof Integer id) {
            sedeActivaId = id;
        } else if (sedeObj instanceof String sedeStr) {
            try {
                sedeActivaId = Integer.parseInt(sedeStr);
            } catch (NumberFormatException e) {
                sedeActivaId = null;
            }
        }

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "stock", BigDecimal.ZERO,
                    "mensaje", "No hay sede activa seleccionada."
            ));
        }

        List<Producto> lista = service.buscarVendiblesPorSede("", sedeActivaId);

        BigDecimal stock = lista.stream()
                .filter(p -> p.getIdProducto().equals(idProducto))
                .map(p -> p.getStockActualDecimal() == null ? BigDecimal.ZERO : p.getStockActualDecimal())
                .findFirst()
                .orElse(BigDecimal.ZERO);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "idProducto", idProducto,
                "idSede", sedeActivaId,
                "stock", stock
        ));
    }

    @GetMapping("/api/atributos")
    @ResponseBody
    public List<Map<String, Object>> apiAtributosPorCategoria(
            @RequestParam(value = "idCategoria", required = false) Integer idCategoria) {

        return catalogoService.listarAtributosPorCategoria(idCategoria);
    }

    // ============================================================
    // MANTENEDOR: CATEGORÍAS
    // ============================================================

    @PostMapping("/categorias")
    public String guardarCategoria(@RequestParam(value = "idCategoria", required = false) Integer idCategoria,
                                   @RequestParam("nombreCategoria") String nombreCategoria,
                                   RedirectAttributes redirectAttrs) {

        try {
            if (idCategoria == null || idCategoria <= 0) {
                catalogoService.registrarCategoria(nombreCategoria);
                redirectAttrs.addFlashAttribute("mensaje", "Categoría registrada correctamente.");
            } else {
                catalogoService.actualizarCategoria(idCategoria, nombreCategoria);
                redirectAttrs.addFlashAttribute("mensaje", "Categoría actualizada correctamente.");
            }

            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }

    @PostMapping("/categorias/estado")
    public String cambiarEstadoCategoria(@RequestParam("idCategoria") Integer idCategoria,
                                         @RequestParam("estado") Integer estado,
                                         RedirectAttributes redirectAttrs) {

        try {
            catalogoService.cambiarEstadoCategoria(idCategoria, estado);
            redirectAttrs.addFlashAttribute("mensaje", "Estado de categoría actualizado correctamente.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }

    // ============================================================
    // MANTENEDOR: SUBCATEGORÍAS
    // ============================================================

    @PostMapping("/subcategorias")
    public String guardarSubcategoria(@RequestParam(value = "idSubcategoria", required = false) Integer idSubcategoria,
                                      @RequestParam("idCategoria") Integer idCategoria,
                                      @RequestParam("nombreSubcategoria") String nombreSubcategoria,
                                      RedirectAttributes redirectAttrs) {

        try {
            if (idSubcategoria == null || idSubcategoria <= 0) {
                catalogoService.registrarSubcategoria(idCategoria, nombreSubcategoria);
                redirectAttrs.addFlashAttribute("mensaje", "Subcategoría registrada correctamente.");
            } else {
                catalogoService.actualizarSubcategoria(idSubcategoria, idCategoria, nombreSubcategoria);
                redirectAttrs.addFlashAttribute("mensaje", "Subcategoría actualizada correctamente.");
            }

            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }

    @PostMapping("/subcategorias/estado")
    public String cambiarEstadoSubcategoria(@RequestParam("idSubcategoria") Integer idSubcategoria,
                                            @RequestParam("estado") Integer estado,
                                            RedirectAttributes redirectAttrs) {

        try {
            catalogoService.cambiarEstadoSubcategoria(idSubcategoria, estado);
            redirectAttrs.addFlashAttribute("mensaje", "Estado de subcategoría actualizado correctamente.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }

    // ============================================================
// MANTENEDOR: MARCAS
// ============================================================

    @PostMapping("/marcas")
    public String guardarMarca(@RequestParam(value = "idMarca", required = false) Integer idMarca,
                               @RequestParam("nombreMarca") String nombreMarca,
                               @RequestParam(value = "descripcion", required = false) String descripcion,
                               RedirectAttributes redirectAttrs) {

        try {
            if (idMarca == null || idMarca <= 0) {
                catalogoService.registrarMarca(nombreMarca, descripcion);
                redirectAttrs.addFlashAttribute("mensaje", "Marca registrada correctamente.");
            } else {
                catalogoService.actualizarMarca(idMarca, nombreMarca, descripcion);
                redirectAttrs.addFlashAttribute("mensaje", "Marca actualizada correctamente.");
            }

            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }


    @PostMapping("/marcas/estado")
    public String cambiarEstadoMarca(@RequestParam("idMarca") Integer idMarca,
                                     @RequestParam("estado") Integer estado,
                                     RedirectAttributes redirectAttrs) {

        try {
            catalogoService.cambiarEstadoMarca(idMarca, estado);
            redirectAttrs.addFlashAttribute("mensaje", "Estado de marca actualizado correctamente.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }

    // ============================================================
    // MANTENEDOR: UNIDADES
    // ============================================================

    @PostMapping("/unidades")
    public String guardarUnidad(@RequestParam(value = "idUnidad", required = false) Integer idUnidad,
                                @RequestParam("nombreUnidad") String nombreUnidad,
                                @RequestParam(value = "abreviatura", required = false) String abreviatura,
                                @RequestParam("tipoCalculo") String tipoCalculo,
                                RedirectAttributes redirectAttrs) {

        try {
            if (idUnidad == null || idUnidad <= 0) {
                catalogoService.registrarUnidad(nombreUnidad, abreviatura, tipoCalculo);
                redirectAttrs.addFlashAttribute("mensaje", "Unidad registrada correctamente.");
            } else {
                catalogoService.actualizarUnidad(idUnidad, nombreUnidad, abreviatura, tipoCalculo);
                redirectAttrs.addFlashAttribute("mensaje", "Unidad actualizada correctamente.");
            }

            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }

    @PostMapping("/unidades/estado")
    public String cambiarEstadoUnidad(@RequestParam("idUnidad") Integer idUnidad,
                                      @RequestParam("estado") Integer estado,
                                      RedirectAttributes redirectAttrs) {

        try {
            catalogoService.cambiarEstadoUnidad(idUnidad, estado);
            redirectAttrs.addFlashAttribute("mensaje", "Estado de unidad actualizado correctamente.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }



    // ============================================================
    // MANTENEDOR: ATRIBUTOS
    // ============================================================

    @PostMapping("/atributos")
    public String guardarAtributo(@RequestParam(value = "idAtributo", required = false) Integer idAtributo,
                                  @RequestParam(value = "idCategoria", required = false) Integer idCategoria,
                                  @RequestParam("nombreAtributo") String nombreAtributo,
                                  @RequestParam(value = "tipoDato", required = false) String tipoDato,
                                  RedirectAttributes redirectAttrs) {

        try {
            if (idAtributo == null || idAtributo <= 0) {
                catalogoService.registrarAtributo(idCategoria, nombreAtributo, tipoDato);
                redirectAttrs.addFlashAttribute("mensaje", "Atributo registrado correctamente.");
            } else {
                catalogoService.actualizarAtributo(idAtributo, idCategoria, nombreAtributo, tipoDato);
                redirectAttrs.addFlashAttribute("mensaje", "Atributo actualizado correctamente.");
            }

            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }

    @PostMapping("/atributos/estado")
    public String cambiarEstadoAtributo(@RequestParam("idAtributo") Integer idAtributo,
                                        @RequestParam("estado") Integer estado,
                                        RedirectAttributes redirectAttrs) {

        try {
            catalogoService.cambiarEstadoAtributo(idAtributo, estado);
            redirectAttrs.addFlashAttribute("mensaje", "Estado de atributo actualizado correctamente.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "success");

        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/interno/productos";
    }


}
