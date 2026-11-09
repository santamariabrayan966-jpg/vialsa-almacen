package com.dentia.service.interno;

import com.dentia.dto.interno.ventas.ClienteDocumentoConsultaDto;
import com.dentia.dto.interno.ventas.DocumentoExternoResultadoDto;
import com.dentia.model.interno.Cliente;
import com.dentia.service.interno.ClienteService;
import org.springframework.stereotype.Service;

@Service
public class ClienteDocumentoVentaService {

    private final ClienteService clienteService;
    private final DocumentoExternoProvider documentoExternoProvider;

    public ClienteDocumentoVentaService(
            ClienteService clienteService,
            DocumentoExternoProvider documentoExternoProvider) {
        this.clienteService = clienteService;
        this.documentoExternoProvider = documentoExternoProvider;
    }

    public ClienteDocumentoConsultaDto consultar(String documento) {
        String documentoNormalizado = ClienteService.normalizarDocumento(documento);
        ClienteService.validarDocumento(documentoNormalizado);

        Cliente interno = clienteService.buscarPorDocumento(documentoNormalizado);
        if (interno != null) return desdeClienteInterno(interno);

        DocumentoExternoResultadoDto externo = documentoExternoProvider.consultar(documentoNormalizado);
        if (externo.encontrado()) {
            String nombre = documentoNormalizado.length() == 11
                    ? externo.razonSocial()
                    : (externo.nombres() + " " + externo.apellidos()).trim();
            return new ClienteDocumentoConsultaDto(true, "API_EXTERNA", null,
                    tipoDocumento(documentoNormalizado), documentoNormalizado, nombre,
                    externo.nombres(), externo.apellidos(), externo.razonSocial(), "", "",
                    externo.direccion(), true, externo.mensaje());
        }

        String origen = externo.disponible() ? "NO_ENCONTRADO" : "MANUAL";
        return new ClienteDocumentoConsultaDto(false, origen, null,
                tipoDocumento(documentoNormalizado), documentoNormalizado, "", "", "", "",
                "", "", "", true, externo.mensaje());
    }

    private ClienteDocumentoConsultaDto desdeClienteInterno(Cliente cliente) {
        boolean ruc = Integer.valueOf(2).equals(cliente.getIdTipoDocumento())
                || cliente.getNro_documento().length() == 11;
        String nombre = ruc
                ? cliente.getRazonSocial()
                : ((cliente.getNombres() == null ? "" : cliente.getNombres()) + " "
                + (cliente.getApellidos() == null ? "" : cliente.getApellidos())).trim();
        String mensaje = cliente.isActivo()
                ? "Cliente encontrado en la base interna."
                : "Cliente registrado pero inactivo. Se conservará su vínculo sin reactivarlo.";
        if (ruc && (cliente.getRazonSocial() == null || cliente.getRazonSocial().isBlank())) {
            mensaje += " Falta la razón social; actualízala en Clientes antes de emitir una factura.";
        }
        if (ruc && (cliente.getDireccion() == null || cliente.getDireccion().isBlank())) {
            mensaje += " Falta la dirección fiscal; actualízala en Clientes antes de emitir una factura.";
        }
        return new ClienteDocumentoConsultaDto(true, "BASE_INTERNA", cliente.getIdClientes(),
                ruc ? "RUC" : "DNI", cliente.getNro_documento(), nombre,
                cliente.getNombres(), cliente.getApellidos(), cliente.getRazonSocial(),
                cliente.getTelefono(), cliente.getCorreo(), cliente.getDireccion(), cliente.isActivo(), mensaje);
    }

    private String tipoDocumento(String documento) {
        return documento.length() == 11 ? "RUC" : "DNI";
    }
}
