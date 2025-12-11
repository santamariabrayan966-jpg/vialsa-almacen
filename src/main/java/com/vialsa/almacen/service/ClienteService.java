package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IClienteDao;
import com.vialsa.almacen.model.Cliente;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

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
        return dao.marcarVip(idCliente);
    }

    public int quitarVip(Integer idCliente) {
        return dao.quitarVip(idCliente);
    }

    public int marcarMoroso(Integer idCliente) {
        return dao.marcarMoroso(idCliente);
    }

    public int quitarMoroso(Integer idCliente) {
        return dao.quitarMoroso(idCliente);
    }

    // ======================================================
    //     ACTIVAR / DESACTIVAR
    // ======================================================

    public int activarCliente(Integer idCliente) {
        return dao.activarCliente(idCliente);
    }

    public int desactivarCliente(Integer idCliente) {
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

    public int registrarMasivo(List<Cliente> clientes) {
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


    // ======================================================
    //     CREAR CLIENTE AUTOMÁTICO (VENTAS)
    // ======================================================
    public Cliente crearAutomatico(Cliente c) {

        Cliente nuevo = new Cliente();
        nuevo.setNro_documento(c.getNro_documento());
        nuevo.setNombres(c.getNombres());
        nuevo.setApellidos(c.getApellidos());
        nuevo.setTelefono(c.getTelefono());
        nuevo.setDireccion(c.getDireccion());
        nuevo.setActivo(true);

        // Registrar en BD
        dao.registrar(nuevo);

        // Obtener el cliente recién guardado
        return dao.buscarPorDocumento(nuevo.getNro_documento());
    }
}
