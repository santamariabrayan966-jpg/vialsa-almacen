package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.CuotaCompra;
import java.util.List;

public interface ICuotaCompraDao {

    // Registrar cuota
    int registrar(CuotaCompra cuota);

    // Listar cuotas según la compra
    List<CuotaCompra> listarPorCompra(int idCompra);

    // Marcar una cuota como pagada
    int marcarComoPagada(int idCuotaCompra);

    // Eliminar cuotas al editar o borrar compra
    void eliminarPorCompra(int idCompra);

    // 🔴 NECESARIO — obtener una cuota por ID
    CuotaCompra buscarPorId(int idCuotaCompra);
}
