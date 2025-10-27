package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Cliente;
import java.util.List;

public interface IClienteDao {

    Cliente buscarPorDocumento(String documento);

    // ✅ Nuevo método para registrar cliente
    int registrar(Cliente cliente);

    // (Opcional) otros métodos como listar(), eliminar(), etc.
}

