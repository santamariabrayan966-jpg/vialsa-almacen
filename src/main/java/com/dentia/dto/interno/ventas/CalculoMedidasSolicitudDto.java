package com.dentia.dto.interno.ventas;

import java.util.List;

public record CalculoMedidasSolicitudDto(
        Integer idProducto,
        String tipoMedida,
        String unidadEntrada,
        Boolean entregarSobrante,
        String observacion,
        List<CorteMedidaDto> cortes,
        String modalidad,
        Integer idPrecioPresentacion,
        Integer cantidadPresentacion,
        String codigoCombinacion
) {
    public CalculoMedidasSolicitudDto(Integer idProducto,
                                      String tipoMedida,
                                      String unidadEntrada,
                                      Boolean entregarSobrante,
                                      String observacion,
                                      List<CorteMedidaDto> cortes) {
        this(idProducto, tipoMedida, unidadEntrada, entregarSobrante, observacion,
                cortes, "COTIZAR_CORTES", null, null, null);
    }

    public record CorteMedidaDto(java.math.BigDecimal ancho,
                                 java.math.BigDecimal alto,
                                 java.math.BigDecimal longitud,
                                 Integer cantidad) {}
}
