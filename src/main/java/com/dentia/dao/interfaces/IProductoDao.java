package com.dentia.dao.interfaces;

import com.dentia.model.interno.Producto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Map;

public interface IProductoDao {

    List<Producto> listar();

    List<Producto> listarActivos();

    List<Producto> listarEliminados();

    Optional<Producto> buscarPorId(Integer idProducto);

    int crear(Producto p);

    int crearYRetornarId(Producto p);

    int actualizar(Producto p);

    int eliminar(Integer idProducto);

    int restaurar(Integer idProducto);

    void descontarStock(int idProducto, BigDecimal cantidad);

    void aumentarStock(int idProducto, BigDecimal cantidad);

    List<Producto> buscar(String filtro);

    List<Producto> buscarVendiblesPorSede(String filtro, Integer idSede);
    List<Producto> listarActivosConStockPorSede(Integer idSede);

    Integer obtenerEstadoProductoDuplicado(Integer idProductoExcluir,
                                           String nombreProducto,
                                           Integer idMarca,
                                           Integer idCategoria,
                                           Integer idSubcategoria);

    String generarCodigoInterno(Integer idCategoria);

    int cambiarEstado(int idProducto, int nuevoEstado);

    BigDecimal obtenerUltimoPrecioCompra(int idProducto);

    BigDecimal obtenerCostoPromedioPorSede(Integer idProducto, Integer idSede);

    List<Producto> buscarPorTexto(String texto);
    List<Producto> listarActivosConStockEnSedes();

    List<Map<String, Object>> obtenerDisponibilidadPorProducto(Integer idProducto);
}
