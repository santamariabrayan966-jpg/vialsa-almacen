package com.dentia.service.interno;

import com.dentia.dao.interfaces.IProveedorDao;
import com.dentia.dto.interno.ProveedorCompraDto;
import com.dentia.dto.interno.ProveedorComprasResponseDto;
import com.dentia.dto.interno.ProveedorComprasResumenDto;
import com.dentia.dto.interno.ProveedorDetalleDto;
import com.dentia.dto.interno.ProveedorPaginaDto;
import com.dentia.dto.interno.ProveedorSedeResumenDto;
import com.dentia.model.interno.Proveedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ProveedorService {

    private static final Logger log = LoggerFactory.getLogger(ProveedorService.class);
    private static final Pattern DOCUMENTO = Pattern.compile("^(?:\\d{8}|\\d{11})$");
    private static final Pattern NOMBRE_PERMITIDO = Pattern.compile("^[\\p{L}0-9 .,'’&/\\-]+$");
    private static final Pattern DIRECCION_PERMITIDA = Pattern.compile("^[\\p{L}0-9 .,#/()'’&\\-]+$");
    private static final Pattern CORREO = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$", Pattern.CASE_INSENSITIVE);
    private static final List<Integer> TAMANIOS_PERMITIDOS = List.of(5, 10, 20, 50);

    public enum ResultadoGuardarProveedor {
        NUEVO, ACTUALIZADO, REACTIVADO, DUPLICADO, INVALIDO, NO_ENCONTRADO, ELIMINADO, ERROR
    }

    public record ResultadoOperacion(
            ResultadoGuardarProveedor resultado,
            String mensaje,
            Map<String, String> errores
    ) {
        public boolean exitoso() {
            return resultado == ResultadoGuardarProveedor.NUEVO
                    || resultado == ResultadoGuardarProveedor.ACTUALIZADO
                    || resultado == ResultadoGuardarProveedor.REACTIVADO;
        }
    }

    private final IProveedorDao proveedorDao;

    public ProveedorService(IProveedorDao proveedorDao) {
        this.proveedorDao = proveedorDao;
    }

    public List<Proveedor> listar() {
        return proveedorDao.listar();
    }

    public ProveedorPaginaDto obtenerPagina(int pagina, int tamanio, String busqueda) {
        int tamanioSeguro = TAMANIOS_PERMITIDOS.contains(tamanio) ? tamanio : 10;
        int paginaSegura = Math.max(pagina, 0);
        String busquedaSegura = busqueda == null ? "" : busqueda.trim();
        long total = proveedorDao.contar(busquedaSegura);
        int totalPaginas = total == 0 ? 0 : (int) Math.ceil((double) total / tamanioSeguro);
        if (totalPaginas > 0 && paginaSegura >= totalPaginas) paginaSegura = totalPaginas - 1;

        List<Proveedor> contenido = proveedorDao.listarPaginado(
                busquedaSegura, tamanioSeguro, paginaSegura * tamanioSeguro);
        return new ProveedorPaginaDto(
                contenido, paginaSegura, tamanioSeguro, total, totalPaginas, busquedaSegura);
    }

    public Proveedor buscarPorId(Integer id) {
        if (id == null || id <= 0) return null;
        return proveedorDao.buscarPorId(id).orElse(null);
    }

    public Proveedor obtenerPorId(Integer id) {
        return buscarPorId(id);
    }

    public ResultadoOperacion guardar(Proveedor proveedor) {
        if (proveedor == null) {
            return resultadoInvalido(Map.of("general", "No se recibieron datos del proveedor."));
        }

        normalizar(proveedor);
        Map<String, String> errores = validar(proveedor);
        if (!errores.isEmpty()) return resultadoInvalido(errores);

        try {
            Integer id = proveedor.getIdProveedor();
            Optional<Proveedor> mismoDocumento = proveedorDao.buscarPorNroDocumento(proveedor.getNroDocumento());

            if (id != null && id > 0) {
                Optional<Proveedor> optExistente = proveedorDao.buscarPorId(id);
                if (optExistente.isEmpty()) {
                    return new ResultadoOperacion(ResultadoGuardarProveedor.NO_ENCONTRADO,
                            "El proveedor que intentas editar ya no existe.", Map.of());
                }

                Proveedor existente = optExistente.get();
                if (Integer.valueOf(2).equals(existente.getEstado())) {
                    return new ResultadoOperacion(ResultadoGuardarProveedor.ELIMINADO,
                            "No se puede editar un proveedor eliminado.", Map.of());
                }
                if (mismoDocumento.isPresent() && !mismoDocumento.get().getIdProveedor().equals(id)) {
                    return duplicado();
                }

                proveedor.setEstado(existente.getEstado());
                proveedor.setFechaRegistro(existente.getFechaRegistro());
                int filas = proveedorDao.actualizar(proveedor);
                return filas > 0
                        ? new ResultadoOperacion(ResultadoGuardarProveedor.ACTUALIZADO,
                        "Proveedor actualizado correctamente.", Map.of())
                        : errorGenerico();
            }

            if (mismoDocumento.isPresent()) {
                Proveedor existente = mismoDocumento.get();
                if (Integer.valueOf(0).equals(existente.getEstado())) {
                    proveedor.setIdProveedor(existente.getIdProveedor());
                    proveedor.setEstado(1);
                    proveedor.setFechaRegistro(existente.getFechaRegistro());
                    int filas = proveedorDao.actualizar(proveedor);
                    return filas > 0
                            ? new ResultadoOperacion(ResultadoGuardarProveedor.REACTIVADO,
                            "Proveedor reactivado y actualizado correctamente.", Map.of())
                            : errorGenerico();
                }
                return duplicado();
            }

            proveedor.setEstado(1);
            int filas = proveedorDao.crear(proveedor);
            return filas > 0
                    ? new ResultadoOperacion(ResultadoGuardarProveedor.NUEVO,
                    "Proveedor guardado correctamente.", Map.of())
                    : errorGenerico();
        } catch (Exception e) {
            log.error("Error al guardar proveedor con documento {}", proveedor.getNroDocumento(), e);
            return errorGenerico();
        }
    }

    public boolean eliminar(Integer id) {
        if (id == null) return false;
        Proveedor proveedor = buscarPorId(id);
        return proveedor != null && !Integer.valueOf(2).equals(proveedor.getEstado())
                && proveedorDao.eliminar(id) > 0;
    }

    public boolean cambiarEstado(Integer id, boolean activo) {
        Proveedor proveedor = buscarPorId(id);
        return proveedor != null && !Integer.valueOf(2).equals(proveedor.getEstado())
                && proveedorDao.cambiarEstado(id, activo) > 0;
    }

    public boolean actualizarEstado(Proveedor proveedor) {
        if (proveedor == null) return false;
        try {
            return cambiarEstado(proveedor.getIdProveedor(), proveedor.isActivo());
        } catch (Exception e) {
            log.error("Error al actualizar el estado del proveedor {}", proveedor.getIdProveedor(), e);
            return false;
        }
    }

    public List<Proveedor> listarActivos() {
        return proveedorDao.listarActivos();
    }

    public ProveedorDetalleDto obtenerDetalle(Integer id, boolean puedeVerCompras) {
        Proveedor proveedor = buscarPorId(id);
        if (proveedor == null || Integer.valueOf(2).equals(proveedor.getEstado())) return null;

        ProveedorComprasResumenDto resumen = puedeVerCompras
                ? proveedorDao.obtenerResumenCompras(id)
                : ProveedorComprasResumenDto.vacio();

        return new ProveedorDetalleDto(
                proveedor.getIdProveedor(), proveedor.getNombreProveedor(), proveedor.getNroDocumento(),
                proveedor.getDireccion(), proveedor.getTelefono(), proveedor.getCorreo(),
                proveedor.getEstado(), proveedor.getFechaRegistro(), resumen.totalCompras(),
                resumen.totalHistorico(), resumen.ultimaCompra(), resumen.sedes(), puedeVerCompras);
    }

    public ProveedorComprasResponseDto obtenerHistorial(
            Integer idProveedor, String busqueda, Integer idSede, String estado,
            LocalDate fechaDesde, LocalDate fechaHasta, int pagina, int tamanio) {

        Proveedor proveedor = buscarPorId(idProveedor);
        if (proveedor == null || Integer.valueOf(2).equals(proveedor.getEstado())) return null;
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta.");
        }

        int paginaSegura = Math.max(pagina, 0);
        int tamanioSeguro = Math.min(Math.max(tamanio, 5), 50);
        long total = proveedorDao.contarCompras(
                idProveedor, busqueda, idSede, estado, fechaDesde, fechaHasta);
        int totalPaginas = total == 0 ? 0 : (int) Math.ceil((double) total / tamanioSeguro);
        if (totalPaginas > 0 && paginaSegura >= totalPaginas) paginaSegura = totalPaginas - 1;

        List<ProveedorCompraDto> compras = proveedorDao.listarCompras(
                idProveedor, busqueda, idSede, estado, fechaDesde, fechaHasta,
                tamanioSeguro, paginaSegura * tamanioSeguro);
        List<ProveedorSedeResumenDto> porSede = proveedorDao.obtenerComprasPorSede(idProveedor);

        return new ProveedorComprasResponseDto(
                proveedorDao.obtenerResumenCompras(idProveedor), porSede, compras,
                proveedorDao.listarEstadosCompras(idProveedor), paginaSegura, tamanioSeguro,
                total, totalPaginas);
    }

    private void normalizar(Proveedor proveedor) {
        proveedor.setNroDocumento(normalizarSimple(proveedor.getNroDocumento()));
        proveedor.setNombreProveedor(normalizarEspacios(proveedor.getNombreProveedor()));
        proveedor.setTelefono(normalizarOpcional(proveedor.getTelefono()));
        String correo = normalizarOpcional(proveedor.getCorreo());
        proveedor.setCorreo(correo == null ? null
                : correo.replaceAll("\\s+", "").toLowerCase(Locale.ROOT));
        proveedor.setDireccion(normalizarOpcionalEspacios(proveedor.getDireccion()));
    }

    private Map<String, String> validar(Proveedor proveedor) {
        Map<String, String> errores = new LinkedHashMap<>();
        String documento = proveedor.getNroDocumento();
        String nombre = proveedor.getNombreProveedor();
        String telefono = proveedor.getTelefono();
        String correo = proveedor.getCorreo();
        String direccion = proveedor.getDireccion();

        if (documento == null || !DOCUMENTO.matcher(documento).matches()) {
            errores.put("nroDocumento", "El documento debe contener exactamente 8 dígitos (DNI) u 11 dígitos (RUC).");
        }
        if (nombre == null || nombre.length() < 3 || nombre.length() > 120) {
            errores.put("nombreProveedor", "El nombre o razón social debe tener entre 3 y 120 caracteres.");
        } else if (!NOMBRE_PERMITIDO.matcher(nombre).matches() || !nombre.matches(".*\\p{L}.*")
                || nombre.matches("(?iu)^([\\p{L}])\\1{2,}$")) {
            errores.put("nombreProveedor", "Ingresa un nombre o razón social real con caracteres válidos.");
        }
        if (telefono != null) {
            if (!telefono.matches("\\d{7,15}")) {
                errores.put("telefono", "El teléfono debe contener entre 7 y 15 dígitos.");
            } else if (telefono.length() == 9 && !telefono.startsWith("9")) {
                errores.put("telefono", "Un celular peruano de 9 dígitos debe comenzar con 9.");
            }
        }
        if (correo != null && (correo.length() > 120 || !CORREO.matcher(correo).matches())) {
            errores.put("correo", "Ingresa un correo válido, por ejemplo contacto@empresa.com.");
        }
        if (direccion != null) {
            if (direccion.length() < 5 || direccion.length() > 180) {
                errores.put("direccion", "La dirección debe tener entre 5 y 180 caracteres.");
            } else if (!DIRECCION_PERMITIDA.matcher(direccion).matches()
                    || !direccion.matches(".*\\p{L}.*")) {
                errores.put("direccion", "Ingresa una dirección válida que incluya texto.");
            }
        }
        return errores;
    }

    private ResultadoOperacion resultadoInvalido(Map<String, String> errores) {
        return new ResultadoOperacion(ResultadoGuardarProveedor.INVALIDO,
                "Revisa los campos marcados antes de guardar.", errores);
    }

    private ResultadoOperacion duplicado() {
        return new ResultadoOperacion(ResultadoGuardarProveedor.DUPLICADO,
                "El documento ya pertenece a otro proveedor.",
                Map.of("nroDocumento", "Este documento ya está registrado."));
    }

    private ResultadoOperacion errorGenerico() {
        return new ResultadoOperacion(ResultadoGuardarProveedor.ERROR,
                "No se pudo guardar el proveedor. Inténtalo nuevamente.", Map.of());
    }

    private String normalizarSimple(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarEspacios(String valor) {
        String normalizado = normalizarSimple(valor);
        return normalizado == null ? null : normalizado.replaceAll("\\s+", " ");
    }

    private String normalizarOpcional(String valor) {
        String normalizado = normalizarSimple(valor);
        return normalizado == null || normalizado.isBlank() ? null : normalizado;
    }

    private String normalizarOpcionalEspacios(String valor) {
        String normalizado = normalizarOpcional(valor);
        return normalizado == null ? null : normalizado.replaceAll("\\s+", " ");
    }
}
