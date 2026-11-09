package com.dentia.controller.interno;

import com.dentia.dto.gestion.GestionRequests.*;
import com.dentia.security.CustomAuthorizationService;
import com.dentia.service.interno.GestionInternaService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/interno/gestion")
public class GestionInternaController {
    private static final Logger log=LoggerFactory.getLogger(GestionInternaController.class);
    private final GestionInternaService service;
    private final CustomAuthorizationService permisos;
    public GestionInternaController(GestionInternaService service,CustomAuthorizationService permisos){this.service=service;this.permisos=permisos;}

    @ModelAttribute
    public void noCache(HttpServletResponse response){response.setHeader("Cache-Control","no-store");response.setHeader("X-Content-Type-Options","nosniff");}

    @GetMapping("/{modulo}")
    public String pagina(@PathVariable String modulo,Authentication auth,Model model){
        autorizar(modulo,auth);model.addAttribute("moduloGestion",modulo);return "interno/gestion/index";
    }

    @GetMapping("/{modulo}/datos") @ResponseBody
    public Map<String,Object> datos(@PathVariable String modulo,Authentication auth,
            @RequestParam(required=false) Long paciente,@RequestParam(required=false) String q,
            @RequestParam(required=false) String estado,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue="0") int pagina){
        autorizar(modulo,auth);return service.listar(modulo,filtro(modulo,paciente,q,estado,desde,hasta,pagina));
    }

    @GetMapping("/{modulo}/opciones") @ResponseBody
    public Map<String,Object> opciones(@PathVariable String modulo,Authentication auth,
            @RequestParam(required=false) Long paciente,@RequestParam(required=false) String q){
        autorizar(modulo,auth);return service.opciones(modulo,paciente,q);
    }

    @GetMapping("/{modulo}/{id}") @ResponseBody
    public Map<String,Object> detalle(@PathVariable String modulo,@PathVariable long id,Authentication auth){
        autorizarRegistro(modulo,auth);return service.detalle(modulo,id);
    }

    @PostMapping("/presupuestos") @ResponseBody
    public Map<String,Long> crearPresupuesto(@RequestBody Presupuesto input,Authentication auth){autorizar("presupuestos",auth);return Map.of("id",service.guardarPresupuesto(null,input,auth.getName()));}
    @PutMapping("/presupuestos/{id}") @ResponseBody
    public Map<String,Long> editarPresupuesto(@PathVariable long id,@RequestBody Presupuesto input,Authentication auth){autorizar("presupuestos",auth);return Map.of("id",service.guardarPresupuesto(id,input,auth.getName()));}
    @PostMapping("/presupuestos/{id}/estado") @ResponseBody
    public Map<String,Boolean> estadoPresupuesto(@PathVariable long id,@RequestBody Estado input,Authentication auth){autorizar("presupuestos",auth);service.estadoPresupuesto(id,input,auth.getName());return Map.of("ok",true);}

    @PostMapping("/pagos") @ResponseBody
    public Map<String,Long> crearPago(@RequestBody Pago input,Authentication auth){autorizar("pagos",auth);return Map.of("id",service.registrarPago(input,auth.getName()));}
    @PostMapping("/pagos/{id}/anular") @ResponseBody
    public Map<String,Boolean> anularPago(@PathVariable long id,@RequestBody Estado input,Authentication auth){autorizar("pagos",auth);service.anularPago(id,input.motivo(),auth.getName());return Map.of("ok",true);}

    @PostMapping("/seguimiento") @ResponseBody
    public Map<String,Long> crearSeguimiento(@RequestBody Seguimiento input,Authentication auth){autorizar("seguimiento",auth);return Map.of("id",service.guardarSeguimiento(null,input,auth.getName()));}
    @PutMapping("/seguimiento/{id}") @ResponseBody
    public Map<String,Long> editarSeguimiento(@PathVariable long id,@RequestBody Seguimiento input,Authentication auth){autorizar("seguimiento",auth);return Map.of("id",service.guardarSeguimiento(id,input,auth.getName()));}
    @PostMapping("/seguimiento/{id}/estado") @ResponseBody
    public Map<String,Boolean> cerrarSeguimiento(@PathVariable long id,@RequestBody Estado input,Authentication auth){autorizar("seguimiento",auth);service.cerrarSeguimiento(id,input,auth.getName());return Map.of("ok",true);}

    @PostMapping(value="/documentos",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseBody
    public Map<String,Long> crearDocumento(@RequestParam Long pacienteId,@RequestParam(required=false) Long citaId,
            @RequestParam(required=false) Long presupuestoId,@RequestParam String titulo,@RequestParam String tipo,
            @RequestParam MultipartFile archivo,Authentication auth) throws IOException {
        autorizar("documentos",auth);return Map.of("id",service.guardarDocumento(pacienteId,citaId,presupuestoId,titulo,tipo,archivo,auth.getName()));
    }
    @PostMapping("/documentos/{id}/estado") @ResponseBody
    public Map<String,Boolean> estadoDocumento(@PathVariable long id,@RequestBody Estado input,Authentication auth){autorizar("documentos",auth);service.estadoDocumento(id,input,auth.getName());return Map.of("ok",true);}
    @GetMapping("/documentos/{id}/archivo")
    public ResponseEntity<byte[]> descargarDocumento(@PathVariable long id,Authentication auth){
        autorizar("documentos",auth);var d=service.descargarDocumento(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.get("mime").toString()))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(d.get("nombre_archivo").toString(),StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff")
                .body((byte[])d.get("contenido"));
    }

    @GetMapping("/reportes/cobros.csv")
    public ResponseEntity<String> exportar(Authentication auth,@RequestParam(required=false) Long paciente,
            @RequestParam(required=false) String q,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta){
        autorizar("reportes",auth);
        return ResponseEntity.ok().contentType(new MediaType("text","csv",StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"dentia-cobros.csv\"")
                .body(service.exportarCobros(filtro("reportes",paciente,q,null,desde,hasta,0)));
    }

    private Filtro filtro(String modulo,Long paciente,String q,String estado,LocalDate desde,LocalDate hasta,int pagina){
        if(modulo.equals("reportes")){if(desde==null)desde=LocalDate.now().withDayOfMonth(1);if(hasta==null)hasta=LocalDate.now();estado=null;}
        return new Filtro(paciente,q,estado,desde,hasta,pagina);
    }
    private void autorizarRegistro(String modulo,Authentication auth){autorizar(modulo,auth);if(modulo.equals("reportes"))throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
    private void autorizar(String modulo,Authentication auth){
        if(!GestionInternaService.MODULOS.contains(modulo))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if(!permisos.tieneAcceso(auth,modulo))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler({IllegalArgumentException.class,HttpMessageNotReadableException.class,MaxUploadSizeExceededException.class})
    @ResponseBody public ResponseEntity<Map<String,String>> invalido(Exception ex){
        String message=ex instanceof IllegalArgumentException?ex.getMessage():ex instanceof MaxUploadSizeExceededException?"El archivo supera el límite de 1 MB.":"Revisa los datos y el formato de las fechas e importes.";
        return ResponseEntity.badRequest().body(Map.of("error",message));
    }
    @ExceptionHandler(NoSuchElementException.class) @ResponseBody
    public ResponseEntity<Map<String,String>> inexistente(){return ResponseEntity.status(404).body(Map.of("error","El registro no existe."));}
    @ExceptionHandler({DataAccessException.class,IOException.class}) @ResponseBody
    public ResponseEntity<Map<String,String>> persistencia(Exception ex){
        log.error("No se pudo completar la operación de Gestión",ex);
        return ResponseEntity.status(503).body(Map.of("error","No se pudo guardar o consultar la información. Actualiza y vuelve a intentarlo; si continúa, contacta al administrador."));
    }
}
