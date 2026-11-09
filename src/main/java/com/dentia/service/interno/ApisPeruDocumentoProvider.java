package com.dentia.service.interno;
import com.dentia.dto.interno.ventas.DocumentoExternoResultadoDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
@Service
@ConditionalOnProperty(name="dentia.documento.provider", havingValue="apisperu", matchIfMissing=true)
public class ApisPeruDocumentoProvider implements DocumentoExternoProvider {
 private final RestTemplate client; private final String token;
 public ApisPeruDocumentoProvider(RestTemplateBuilder builder, @Value("${apisperu.token:}") String token) { this.client=builder.setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(10)).build(); this.token=token; }
 private String value(JsonNode n, String... keys) { for(String key:keys) if(n.hasNonNull(key) && !n.get(key).asText().isBlank()) return n.get(key).asText().trim(); return ""; }
 public DocumentoExternoResultadoDto consultar(String documento) {
  if(token.isBlank()) return DocumentoExternoResultadoDto.noDisponible("Configura APISPERU_TOKEN en el servidor.");
  try {
   HttpHeaders h=new HttpHeaders(); h.setBearerAuth(token);
   JsonNode root=client.exchange("https://dniruc.apisperu.com/api/v1/"+(documento.length()==8?"dni/":"ruc/")+documento,HttpMethod.GET,new HttpEntity<>(h),JsonNode.class).getBody();
   if(root==null || (root.path("success").isBoolean() && !root.path("success").asBoolean())) return DocumentoExternoResultadoDto.noEncontrado();
   JsonNode d=root.hasNonNull("data")?root.get("data"):root.hasNonNull("datos")?root.get("datos"):root;
   String nombres=value(d,"nombres","nombre"), apellidos=(value(d,"apellidoPaterno","apellido_paterno","ape_paterno")+" "+value(d,"apellidoMaterno","apellido_materno","ape_materno")).trim();
   if(apellidos.isBlank()) apellidos=value(d,"apellidos");
   String razon=value(d,"razonSocial","razon_social","nombre_o_razon_social"), direccion=value(d,"direccion","domicilio");
   if((documento.length()==8?nombres+apellidos:razon).isBlank()) return DocumentoExternoResultadoDto.noEncontrado();
   return new DocumentoExternoResultadoDto(true,true,nombres,apellidos,razon,direccion,"Datos encontrados.");
  } catch(HttpClientErrorException.NotFound e) { return DocumentoExternoResultadoDto.noEncontrado(); }
  catch(Exception e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Consulta APIsPERU no disponible: {}",e.getClass().getSimpleName()); return DocumentoExternoResultadoDto.noDisponible("No fue posible consultar el documento."); }
 }
}
