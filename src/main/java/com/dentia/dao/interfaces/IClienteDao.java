package com.dentia.dao.interfaces;

import com.dentia.dto.interno.clientes.ClienteCompraDto;
import com.dentia.dto.interno.clientes.ClienteRankingDto;
import com.dentia.dto.interno.clientes.ClienteResumenComercialDto;
import com.dentia.model.interno.Cliente;
import java.time.LocalDate;
import java.util.List;

public interface IClienteDao {

    // =============================
    // EXISTENTES
    // =============================

    // 🔍 Buscar por DNI o RUC
    Cliente buscarPorDocumento(String documento);

    Cliente buscarPorDocumentoParaActualizar(String documento);

    // 🔍 Buscar por correo (para login Google)
    Cliente buscarPorCorreo(String correo);

    // 🔍 Buscar por ID
    Cliente buscarPorId(Integer idCliente);

    // ➕ Registrar cliente
    int registrar(Cliente cliente);

    // ✏ Actualizar cliente
    int actualizar(Cliente cliente);

    // ❌ Eliminar cliente
    int eliminar(Integer idCliente);

    // 📋 Listar todos
    List<Cliente> listarTodos();

    // 🔎 Búsqueda avanzada
    List<Cliente> buscarClientes(String filtro);

    List<Cliente> listarPagina(String busqueda, String filtro, int limite, int offset);

    long contarClientes(String busqueda, String filtro);


    // =============================
    // NUEVOS MÓDULOS PRO
    // =============================

    // ⭐ Etiquetas / Categorías
    int marcarVip(Integer idCliente);
    int quitarVip(Integer idCliente);

    int marcarMoroso(Integer idCliente);
    int quitarMoroso(Integer idCliente);

    // 🟢 Activar / Desactivar cliente
    int activarCliente(Integer idCliente);
    int desactivarCliente(Integer idCliente);

    // 📝 Notas internas
    int agregarNota(Integer idCliente, String nota);
    List<String> obtenerNotas(Integer idCliente);

    // 🕒 Historial del cliente
    int registrarHistorial(Integer idCliente, String accion);
    List<String> obtenerHistorial(Integer idCliente);

    // 📊 Perfil completo (vista avanzada)
    Cliente obtenerPerfilCompleto(Integer idCliente);

    // 📥 Importar clientes (Excel/CSV)
    int registrarMasivo(List<Cliente> clientes);

    // 📤 Exportar clientes
    List<Cliente> listarParaExportar();

    // 🔎 Filtros avanzados (VIP, moroso, inactivo, nuevos, etc.)
    List<Cliente> filtrarClientes(String tipoFiltro);
    // Registrar cliente automáticamente (ventas)
    Cliente crearAutomatico(Cliente cliente);

    Cliente buscarPorGoogleSub(String googleSub);

    int actualizarDatosGoogle(Cliente cliente);

    List<ClienteResumenComercialDto> listarResumenesComerciales();

    List<ClienteResumenComercialDto> listarResumenesComercialesPorIds(List<Integer> idsClientes);

    ClienteResumenComercialDto obtenerResumenComercial(Integer idCliente);

    List<ClienteCompraDto> listarComprasCliente(
            Integer idCliente, String busqueda, String canal, String estado,
            Integer idSede, LocalDate fecha, Integer mes, Integer anio, LocalDate fechaDesde,
            LocalDate fechaHasta, int limite, int offset);

    long contarComprasCliente(
            Integer idCliente, String busqueda, String canal, String estado,
            Integer idSede, LocalDate fecha, Integer mes, Integer anio, LocalDate fechaDesde,
            LocalDate fechaHasta);

    ClienteResumenComercialDto resumirComprasCliente(
            Integer idCliente, String busqueda, String canal, String estado,
            Integer idSede, LocalDate fecha, Integer mes, Integer anio, LocalDate fechaDesde,
            LocalDate fechaHasta);

    List<ClienteRankingDto> obtenerRanking(
            Integer mes, Integer anio, String canal, Integer idSede,
            String criterio, int limite);

    List<Integer> listarAniosConCompras();


}
