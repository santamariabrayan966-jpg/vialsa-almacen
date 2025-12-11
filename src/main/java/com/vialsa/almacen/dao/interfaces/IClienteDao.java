package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Cliente;
import java.util.List;

public interface IClienteDao {

    // =============================
    // EXISTENTES
    // =============================

    // 🔍 Buscar por DNI o RUC
    Cliente buscarPorDocumento(String documento);

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


}
