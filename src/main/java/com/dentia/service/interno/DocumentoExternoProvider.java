package com.dentia.service.interno;

import com.dentia.dto.interno.ventas.DocumentoExternoResultadoDto;

public interface DocumentoExternoProvider {
    DocumentoExternoResultadoDto consultar(String documento);
}
