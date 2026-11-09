package com.dentia.service.interno;

import com.dentia.dao.interfaces.IClienteDao;
import com.dentia.dto.interno.clientes.ClienteCompraDto;
import com.dentia.dto.interno.clientes.ClienteComprasPaginaDto;
import com.dentia.dto.interno.clientes.ClienteDetalleComercialDto;
import com.dentia.dto.interno.clientes.ClientePaginaDto;
import com.dentia.dto.interno.clientes.ClienteRankingDto;
import com.dentia.dto.interno.clientes.ClienteResumenComercialDto;
import com.dentia.dto.interno.ventas.ClienteVentaSolicitudDto;
import com.dentia.model.interno.Cliente;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private static final Set<String> NOMBRES_NO_VALIDOS = Set.of(
            "CLIENTE", "PRUEBA", "SIN NOMBRE", "CONSUMIDOR", "CONSUMIDOR FINAL",
            "N A", "NA", "AAA", "123");

    private final IClienteDao dao;

    public ClienteService(IClienteDao dao) {
        this.dao = dao;
    }

    // ======================================================
    //     BÚSQUEDAS
    // ======================================================

    public Cliente buscarPorDocumento(String documento) {
        return dao.buscarPorDocumento(documento);
    }

    public Cliente buscarPorCorreo(String correo) {
        return dao.buscarPorCorreo(correo);
    }

    public boolean existeDocumento(String documento) {
        return dao.buscarPorDocumento(documento) != null;
    }

    public static String normalizarDocumento(String documento) {
        return documento == null ? "" : documento.trim();
    }

    public static void validarDocumento(String documento) {
        if (documento == null || !(documento.matches("\\d{8}") || documento.matches("\\d{11}"))) {
            throw new IllegalArgumentException("El documento debe contener exactamente 8 dígitos para DNI o 11 para RUC.");
        }
    }

    @Transactional
    public Cliente resolverClienteParaVenta(ClienteVentaSolicitudDto solicitud) {
        String documento = normalizarDocumento(solicitud.documento());
        validarDocumento(documento);

        Cliente porDocumento = dao.buscarPorDocumentoParaActualizar(documento);
        if (solicitud.idCliente() != null && solicitud.idCliente() > 0) {
            Cliente porId = dao.buscarPorId(solicitud.idCliente());
            if (porId == null || !documento.equals(normalizarDocumento(porId.getNro_documento()))) {
                throw new IllegalArgumentException("El cliente seleccionado no corresponde al documento ingresado.");
            }
            if (porDocumento != null && !porDocumento.getIdClientes().equals(porId.getIdClientes())) {
                throw new IllegalArgumentException("El documento ya pertenece a otro cliente.");
            }
            validarClienteParaComprobante(porId, solicitud.tipoComprobante());
            return porId;
        }

        if (porDocumento != null) {
            validarClienteParaComprobante(porDocumento, solicitud.tipoComprobante());
            return porDocumento;
        }

        Cliente nuevo = construirClienteVenta(solicitud, documento);
        try {
            dao.registrar(nuevo);
        } catch (DuplicateKeyException ex) {
            Cliente concurrente = dao.buscarPorDocumentoParaActualizar(documento);
            if (concurrente != null) {
                validarClienteParaComprobante(concurrente, solicitud.tipoComprobante());
                return concurrente;
            }
            throw ex;
        }

        Cliente creado = dao.buscarPorDocumentoParaActualizar(documento);
        if (creado == null) throw new IllegalStateException("No se pudo recuperar el cliente creado.");
        validarClienteParaComprobante(creado, solicitud.tipoComprobante());
        return creado;
    }

    public String nombreParaVenta(Cliente cliente) {
        if (cliente == null) return "";
        boolean ruc = Integer.valueOf(2).equals(cliente.getIdTipoDocumento())
                || normalizarDocumento(cliente.getNro_documento()).length() == 11;
        return ruc ? textoOBlanco(cliente.getRazonSocial())
                : (textoOBlanco(cliente.getNombres()) + " " + textoOBlanco(cliente.getApellidos())).trim();
    }

    public void validarNombreVenta(String nombre) {
        validarNombreReal(nombre, "nombre");
    }

    private Cliente construirClienteVenta(ClienteVentaSolicitudDto solicitud, String documento) {
        boolean ruc = documento.length() == 11;
        String nombreMostrado = texto(solicitud.nombreMostrado());
        String nombres = texto(solicitud.nombres());
        String apellidos = texto(solicitud.apellidos());
        String razonSocial = texto(solicitud.razonSocial());
        if (ruc && razonSocial == null) razonSocial = nombreMostrado;
        if (!ruc && nombres == null) nombres = nombreMostrado;

        validarNombreReal(ruc ? razonSocial : (textoOBlanco(nombres) + " " + textoOBlanco(apellidos)).trim(),
                ruc ? "razón social" : "nombre");
        validarContacto(solicitud.telefono(), solicitud.correo());
        if ("FACTURA".equalsIgnoreCase(solicitud.tipoComprobante()) && texto(solicitud.direccion()) == null) {
            throw new IllegalArgumentException("La dirección fiscal es obligatoria para emitir una factura.");
        }

        Cliente cliente = new Cliente();
        cliente.setNro_documento(documento);
        cliente.setIdTipoDocumento(ruc ? 2 : 1);
        cliente.setNombres(ruc ? null : nombres);
        cliente.setApellidos(ruc ? null : textoOBlanco(apellidos));
        cliente.setRazonSocial(ruc ? razonSocial : null);
        cliente.setTelefono(texto(solicitud.telefono()));
        cliente.setCorreo(texto(solicitud.correo()));
        cliente.setDireccion(texto(solicitud.direccion()));
        cliente.setActivo(true);
        return cliente;
    }

    private void validarClienteParaComprobante(Cliente cliente, String tipoComprobante) {
        String documento = normalizarDocumento(cliente.getNro_documento());
        validarDocumento(documento);
        if ("BOLETA".equalsIgnoreCase(tipoComprobante) && documento.length() != 8) {
            throw new IllegalArgumentException("La boleta requiere un DNI válido de 8 dígitos.");
        }
        if ("FACTURA".equalsIgnoreCase(tipoComprobante)) {
            if (documento.length() != 11) {
                throw new IllegalArgumentException("La factura requiere un RUC válido de 11 dígitos.");
            }
            validarNombreReal(cliente.getRazonSocial(), "razón social");
            if (texto(cliente.getDireccion()) == null) {
                throw new IllegalArgumentException("La dirección fiscal es obligatoria para emitir una factura.");
            }
        } else {
            validarNombreReal(nombreParaVenta(cliente), "nombre");
        }
    }

    private void validarNombreReal(String valor, String campo) {
        String limpio = texto(valor);
        if (limpio == null || limpio.length() < 3 || !limpio.matches(".*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ].*")) {
            throw new IllegalArgumentException("El " + campo + " debe contener información real y suficiente.");
        }
        String comparable = Normalizer.normalize(limpio, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim().toUpperCase();
        if (NOMBRES_NO_VALIDOS.contains(comparable)) {
            throw new IllegalArgumentException("El " + campo + " ingresado no es válido para un comprobante.");
        }
    }

    private void validarContacto(String telefono, String correo) {
        String telefonoLimpio = texto(telefono);
        if (telefonoLimpio != null && !telefonoLimpio.matches("\\d{7,15}")) {
            throw new IllegalArgumentException("El teléfono debe contener entre 7 y 15 dígitos.");
        }
        String correoLimpio = texto(correo);
        if (correoLimpio != null && !correoLimpio.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("El correo electrónico no tiene un formato válido.");
        }
    }

    public Cliente buscarPorId(Integer id) {
        return dao.buscarPorId(id);
    }

    public List<Cliente> listarTodos() {
        return dao.listarTodos();
    }

    public List<Cliente> buscar(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return listarTodos();
        }
        return dao.buscarClientes(filtro.trim());
    }

    public ClientePaginaDto obtenerPagina(String busqueda, String filtro, int pagina, int tamano) {
        int tamanoSeguro = tamano == 20 || tamano == 50 ? tamano : 10;
        int paginaSolicitada = Math.max(0, pagina);
        String filtroSeguro = normalizarFiltroListado(filtro);
        String busquedaSegura = busqueda == null ? "" : busqueda.trim();
        long total = dao.contarClientes(busquedaSegura, filtroSeguro);
        int totalPaginas = total == 0 ? 0 : (int) Math.ceil((double) total / tamanoSeguro);
        int paginaSegura = totalPaginas == 0 ? 0 : Math.min(paginaSolicitada, totalPaginas - 1);
        List<Cliente> clientes = dao.listarPagina(busquedaSegura, filtroSeguro, tamanoSeguro, paginaSegura * tamanoSeguro);
        List<Integer> ids = clientes.stream().map(Cliente::getIdClientes).toList();
        Map<Integer, ClienteResumenComercialDto> resumenes = dao.listarResumenesComercialesPorIds(ids).stream()
                .collect(Collectors.toMap(ClienteResumenComercialDto::idCliente, resumen -> resumen,
                        (primero, segundo) -> primero, LinkedHashMap::new));
        long desde = total == 0 ? 0 : (long) paginaSegura * tamanoSeguro + 1;
        long hasta = Math.min(total, (long) (paginaSegura + 1) * tamanoSeguro);
        return new ClientePaginaDto(clientes, resumenes, paginaSegura, tamanoSeguro, total, totalPaginas, desde, hasta);
    }

    public long contarClientes(String filtro) {
        return dao.contarClientes("", normalizarFiltroListado(filtro));
    }

    // ======================================================
    //     CRUD
    // ======================================================

    public int crear(Cliente cliente) {
        return dao.registrar(cliente);
    }

    public int actualizar(Cliente cliente) {
        return dao.actualizar(cliente);
    }

    public int eliminar(Integer id) {
        return dao.eliminar(id);
    }

    // ======================================================
    //     ETIQUETAS (VIP / MOROSO)
    // ======================================================

    public int marcarVip(Integer idCliente) {
        asegurarCliente(idCliente);
        return dao.marcarVip(idCliente);
    }

    public int quitarVip(Integer idCliente) {
        asegurarCliente(idCliente);
        return dao.quitarVip(idCliente);
    }

    public int marcarMoroso(Integer idCliente) {
        asegurarCliente(idCliente);
        return dao.marcarMoroso(idCliente);
    }

    public int quitarMoroso(Integer idCliente) {
        asegurarCliente(idCliente);
        return dao.quitarMoroso(idCliente);
    }

    // ======================================================
    //     ACTIVAR / DESACTIVAR
    // ======================================================

    public int activarCliente(Integer idCliente) {
        asegurarCliente(idCliente);
        return dao.activarCliente(idCliente);
    }

    public int desactivarCliente(Integer idCliente) {
        asegurarCliente(idCliente);
        return dao.desactivarCliente(idCliente);
    }

    // ======================================================
    //     NOTAS INTERNAS
    // ======================================================

    public int agregarNota(Integer idCliente, String nota) {
        return dao.agregarNota(idCliente, nota);
    }

    public List<String> obtenerNotas(Integer idCliente) {
        return dao.obtenerNotas(idCliente);
    }

    // ======================================================
    //     HISTORIAL DEL CLIENTE
    // ======================================================

    public int registrarHistorial(Integer idCliente, String accion) {
        return dao.registrarHistorial(idCliente, accion);
    }

    public List<String> obtenerHistorial(Integer idCliente) {
        return dao.obtenerHistorial(idCliente);
    }

    // ======================================================
    //     PERFIL COMPLETO
    // ======================================================

    public Cliente obtenerPerfilCompleto(Integer idCliente) {
        return dao.obtenerPerfilCompleto(idCliente);
    }

    // ======================================================
    //     IMPORTAR MASIVO
    // ======================================================

    @Transactional
    public int registrarMasivo(List<Cliente> clientes) {
        if (clientes == null || clientes.isEmpty()) {
            throw new IllegalArgumentException("El archivo no contiene clientes");
        }
        java.util.Set<String> documentos = new java.util.HashSet<>();
        for (Cliente cliente : clientes) {
            String documento = texto(cliente.getNro_documento());
            if (documento == null || !(documento.matches("\\d{8}") || documento.matches("\\d{11}"))) {
                throw new IllegalArgumentException("Todos los clientes deben tener DNI de 8 o RUC de 11 dígitos");
            }
            if (!documentos.add(documento)) {
                throw new IllegalArgumentException("El archivo contiene documentos duplicados");
            }
            if (dao.buscarPorDocumento(documento) != null) {
                throw new IllegalArgumentException("Ya existe un cliente con documento " + documento);
            }
        }
        return dao.registrarMasivo(clientes);
    }

    // ======================================================
    //     EXPORTAR
    // ======================================================

    public List<Cliente> listarParaExportar() {
        return dao.listarParaExportar();
    }

    // ======================================================
    //     FILTROS AVANZADOS
    // ======================================================

    public List<Cliente> filtrar(String tipoFiltro) {
        return dao.filtrarClientes(tipoFiltro);
    }

    public Map<Integer, ClienteResumenComercialDto> obtenerResumenesComerciales() {
        return dao.listarResumenesComerciales().stream()
                .collect(Collectors.toMap(ClienteResumenComercialDto::idCliente, resumen -> resumen,
                        (primero, segundo) -> primero, LinkedHashMap::new));
    }

    public ClienteDetalleComercialDto obtenerDetalleComercial(Integer idCliente) {
        Cliente cliente = dao.obtenerPerfilCompleto(idCliente);
        if (cliente == null) {
            cliente = dao.buscarPorId(idCliente);
        }
        if (cliente == null) {
            throw new NoSuchElementException("Cliente no encontrado");
        }

        ClienteResumenComercialDto resumen = dao.obtenerResumenComercial(idCliente);
        String nombre = texto(cliente.getRazonSocial());
        if (nombre == null) {
            nombre = String.join(" ", textoOBlanco(cliente.getNombres()), textoOBlanco(cliente.getApellidos())).trim();
        }
        if (nombre.isBlank()) nombre = "Cliente";

        return new ClienteDetalleComercialDto(
                cliente.getIdClientes(), nombre,
                texto(cliente.getTipoDocumentoNombre()) != null ? cliente.getTipoDocumentoNombre() :
                        (Integer.valueOf(2).equals(cliente.getIdTipoDocumento()) ? "RUC" : "DNI"),
                cliente.getNro_documento(), cliente.getTelefono(), cliente.getCorreo(), cliente.getDireccion(),
                cliente.isActivo(), cliente.isVip(), cliente.isMoroso(),
                cliente.getFechaRegistro() != null ? cliente.getFechaRegistro().toLocalDateTime() : null,
                cliente.getFoto(), resumen.clasificacion(), resumen.cantidadCompras(),
                resumen.totalHistorico(), resumen.ticketPromedio(), resumen.primeraCompra(), resumen.ultimaCompra(),
                resumen.sedePrincipal(), resumen.canalPrincipal(), resumen.metodoPagoPrincipal()
        );
    }

    public ClienteComprasPaginaDto obtenerComprasCliente(
            Integer idCliente, String busqueda, String canal, String estado, Integer idSede,
            LocalDate fecha, Integer mes, Integer anio, LocalDate fechaDesde, LocalDate fechaHasta,
            int pagina, int tamano) {
        validarFiltrosCompras(canal, estado, fecha, mes, anio, fechaDesde, fechaHasta);
        if (dao.buscarPorId(idCliente) == null) {
            throw new NoSuchElementException("Cliente no encontrado");
        }
        int tamanoSeguro = Math.max(5, Math.min(tamano, 50));
        long total = dao.contarComprasCliente(idCliente, busqueda, canal, estado, idSede, fecha, mes, anio, fechaDesde, fechaHasta);
        int totalPaginas = total == 0 ? 0 : (int) Math.ceil((double) total / tamanoSeguro);
        int paginaSegura = totalPaginas == 0 ? 0 : Math.min(Math.max(0, pagina), totalPaginas - 1);
        List<ClienteCompraDto> compras = dao.listarComprasCliente(idCliente, busqueda, canal, estado, idSede,
                fecha, mes, anio, fechaDesde, fechaHasta, tamanoSeguro, paginaSegura * tamanoSeguro);
        ClienteResumenComercialDto resumen = dao.resumirComprasCliente(idCliente, busqueda, canal, estado,
                idSede, fecha, mes, anio, fechaDesde, fechaHasta);
        return new ClienteComprasPaginaDto(compras, paginaSegura, tamanoSeguro, total, totalPaginas,
                resumen.cantidadCompras(), resumen.totalHistorico(), resumen.ticketPromedio(),
                resumen.ultimaCompra(), resumen.cantidadCanales(), resumen.cantidadSedes());
    }

    public List<ClienteRankingDto> obtenerRanking(Integer mes, Integer anio, String canal,
                                                   Integer idSede, String criterio, int limite) {
        if (mes != null && (mes < 1 || mes > 12)) throw new IllegalArgumentException("Mes inválido");
        if (anio != null && (anio < 2000 || anio > LocalDate.now().getYear() + 1)) throw new IllegalArgumentException("Año inválido");
        if (canal != null && !List.of("TODOS", "ONLINE", "PRESENCIAL", "MIXTO").contains(canal.toUpperCase())) {
            throw new IllegalArgumentException("Canal inválido");
        }
        if (criterio != null && !List.of("MONTO", "COMPRAS").contains(criterio.toUpperCase())) {
            throw new IllegalArgumentException("Criterio inválido");
        }
        int limiteSeguro = limite == 20 || limite == 50 ? limite : 10;
        Map<Integer, ClienteResumenComercialDto> resumenes = obtenerResumenesComerciales();
        return dao.obtenerRanking(mes, anio, canal, idSede, criterio, limiteSeguro).stream()
                .map(item -> {
                    ClienteResumenComercialDto resumen = resumenes.get(item.idCliente());
                    return new ClienteRankingDto(item.posicion(), item.idCliente(), item.cliente(), item.documento(),
                            resumen != null ? resumen.clasificacion() : "SIN COMPRAS", item.cantidadCompras(),
                            item.totalComprado(), item.ticketPromedio(), item.ultimaCompra(),
                            resumen != null ? resumen.sedePrincipal() : null,
                            resumen != null ? resumen.canalPrincipal() : null);
                }).toList();
    }

    public List<Integer> listarAniosConCompras() {
        return dao.listarAniosConCompras();
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void asegurarCliente(Integer idCliente) {
        if (idCliente == null || dao.buscarPorId(idCliente) == null) {
            throw new NoSuchElementException("Cliente no encontrado");
        }
    }

    private void validarFiltrosCompras(String canal, String estado, LocalDate fecha, Integer mes, Integer anio,
                                        LocalDate fechaDesde, LocalDate fechaHasta) {
        if (canal != null && !List.of("TODOS", "ONLINE", "PRESENCIAL").contains(canal.toUpperCase())) {
            throw new IllegalArgumentException("Canal inválido");
        }
        if (estado != null && !List.of("TODOS", "PAGADO", "CREDITO").contains(estado.toUpperCase())) {
            throw new IllegalArgumentException("Estado inválido");
        }
        if (mes != null && (mes < 1 || mes > 12)) throw new IllegalArgumentException("Mes inválido");
        if (anio != null && (anio < 2000 || anio > LocalDate.now().getYear() + 1)) throw new IllegalArgumentException("Año inválido");
        if (fecha == null && fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
    }

    private String normalizarFiltroListado(String filtro) {
        String normalizado = filtro == null ? "TODOS" : filtro.trim().toUpperCase();
        return List.of("TODOS", "VIP", "MOROSO", "INACTIVO", "NUEVO", "ONLINE", "PRESENCIAL", "MIXTO", "SIN_COMPRAS")
                .contains(normalizado) ? normalizado : "TODOS";
    }

    private String textoOBlanco(String valor) {
        String normalizado = texto(valor);
        return normalizado != null ? normalizado : "";
    }


    // ======================================================
    //     CREAR CLIENTE AUTOMÁTICO (VENTAS)
    // ======================================================
    public Cliente crearAutomatico(Cliente c) {

        Cliente nuevo = new Cliente();

        nuevo.setNro_documento(c.getNro_documento());
        nuevo.setTelefono(c.getTelefono());
        nuevo.setDireccion(c.getDireccion());
        nuevo.setActivo(true);

        // DNI
        if (c.getNro_documento() != null &&
                c.getNro_documento().length() == 8) {

            nuevo.setIdTipoDocumento(1);

            nuevo.setNombres(c.getNombres());
            nuevo.setApellidos(c.getApellidos());

        }
        // RUC
        else if (c.getNro_documento() != null &&
                c.getNro_documento().length() == 11) {

            nuevo.setIdTipoDocumento(2);

            nuevo.setRazonSocial(c.getNombres());

            nuevo.setNombres(null);
            nuevo.setApellidos(null);
        }

        dao.registrar(nuevo);

        return dao.buscarPorDocumento(nuevo.getNro_documento());
    }

    public Cliente buscarPorGoogleSub(String googleSub) {
        return dao.buscarPorGoogleSub(googleSub);
    }

    public int actualizarDatosGoogle(Cliente cliente) {
        return dao.actualizarDatosGoogle(cliente);
    }
}
