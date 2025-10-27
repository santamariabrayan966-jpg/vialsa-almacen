package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Proveedor;

import java.util.List;
import java.util.Optional;

public interface IProveedorDao {
    List<Proveedor> listar();
    Optional<Proveedor> buscarPorId(int id);
    int crear(Proveedor proveedor);
    int actualizar(Proveedor proveedor);
    int eliminar(int id);
}
