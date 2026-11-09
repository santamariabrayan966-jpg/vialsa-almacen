package com.dentia.dao.interfaces;

import com.dentia.model.interno.inventario.InventarioAjuste;
import com.dentia.model.interno.inventario.InventarioAjusteDetalle;
import java.util.Optional;

import java.util.List;

public interface IInventarioAjusteDao {

    int registrarAjuste(InventarioAjuste ajuste);

    void registrarDetalle(InventarioAjusteDetalle detalle);

    List<InventarioAjuste> listarAjustes();

    List<InventarioAjuste> listarAjustesPorSede(Integer idSede);

    List<InventarioAjusteDetalle> listarDetalles(Integer idAjuste);
    Optional<InventarioAjuste> buscarPorId(Integer idAjuste);

    String generarCodigo();
}
