package com.dentia.service.interno;

import com.dentia.dao.interfaces.ISedeDao;
import com.dentia.model.interno.Sede;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SedeService {

    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    public enum ResultadoGuardarSede {
        NUEVO,
        ACTUALIZADO,
        DUPLICADO,
        ERROR
    }

    private final ISedeDao sedeDao;

    public SedeService(ISedeDao sedeDao) {
        this.sedeDao = sedeDao;
    }

    public List<Sede> listar() {
        return sedeDao.listar();
    }

    public List<Sede> listarActivas() {
        return sedeDao.listar()
                .stream()
                .filter(Sede::isActivo)
                .toList();
    }

    public Sede buscarPorId(Integer id) {
        if (id == null) return null;
        return sedeDao.buscarPorId(id).orElse(null);
    }

    public ResultadoGuardarSede guardar(Sede sede) {
        try {
            normalizar(sede);

            if (sede.getNombre() == null || sede.getNombre().isBlank()) return ResultadoGuardarSede.ERROR;
            if (sede.getCodigo() == null || sede.getCodigo().isBlank()) return ResultadoGuardarSede.ERROR;
            if (sede.getTipo() == null || sede.getTipo().isBlank()) return ResultadoGuardarSede.ERROR;
            if (sede.getCiudad() == null || sede.getCiudad().isBlank()) return ResultadoGuardarSede.ERROR;
            if (sede.getDireccion() == null || sede.getDireccion().isBlank()) return ResultadoGuardarSede.ERROR;
            if (sede.getTelefono() == null || sede.getTelefono().isBlank()) return ResultadoGuardarSede.ERROR;

            if (sede.getTelefono().length() < 6 || sede.getTelefono().length() > 20) {
                return ResultadoGuardarSede.ERROR;
            }

            if (sede.getResponsable() != null && !sede.getResponsable().isBlank()
                    && sede.getResponsable().length() < 3) return ResultadoGuardarSede.ERROR;

            if (sede.getGoogleMapsUrl() != null && !sede.getGoogleMapsUrl().isBlank()
                    && !sede.getGoogleMapsUrl().startsWith("http://")
                    && !sede.getGoogleMapsUrl().startsWith("https://")) return ResultadoGuardarSede.ERROR;

            Integer id = sede.getIdSede();

            Optional<Sede> existenteCodigo = sedeDao.buscarPorCodigo(sede.getCodigo());

            if (existenteCodigo.isPresent()) {
                Sede existente = existenteCodigo.get();

                if (id == null || !existente.getIdSede().equals(id)) {
                    return ResultadoGuardarSede.DUPLICADO;
                }
            }

            if (id != null && id > 0) {
                Sede existente = buscarPorId(id);
                if (existente == null) {
                    return ResultadoGuardarSede.ERROR;
                }

                sede.setActivo(existente.isActivo());


                int filas = sedeDao.actualizar(sede);
                return filas > 0
                        ? ResultadoGuardarSede.ACTUALIZADO
                        : ResultadoGuardarSede.ERROR;
            }

            sede.setActivo(true);
            int filas = sedeDao.crear(sede);

            return filas > 0
                    ? ResultadoGuardarSede.NUEVO
                    : ResultadoGuardarSede.ERROR;

        } catch (Exception e) {
            e.printStackTrace();
            return ResultadoGuardarSede.ERROR;
        }
    }

    private void normalizar(Sede sede) {
        if (sede.getNombre() != null) sede.setNombre(sede.getNombre().trim());
        if (sede.getCodigo() != null) sede.setCodigo(sede.getCodigo().trim().toUpperCase());
        if (sede.getTipo() != null) sede.setTipo(sede.getTipo().trim().toUpperCase());
        if (sede.getDireccion() != null) sede.setDireccion(sede.getDireccion().trim());
        if (sede.getCiudad() != null) sede.setCiudad(sede.getCiudad().trim());
        if (sede.getResponsable() != null) sede.setResponsable(sede.getResponsable().trim());
        if (sede.getTelefono() != null) sede.setTelefono(sede.getTelefono().trim());
        if (sede.getDescripcion() != null) sede.setDescripcion(sede.getDescripcion().trim());
        if (sede.getGoogleMapsUrl() != null) sede.setGoogleMapsUrl(sede.getGoogleMapsUrl().trim());
    }

    public boolean cambiarEstado(Integer id, boolean activo) {
        if (id == null) return false;
        return sedeDao.cambiarEstado(id, activo) > 0;
    }

    public boolean eliminar(Integer id) {
        if (id == null) return false;
        return sedeDao.eliminar(id) > 0;
    }

    public int totalSedes() {
        return sedeDao.contarTodas();
    }

    public int totalAlmacenes() {
        return sedeDao.contarPorTipo("ALMACEN");
    }

    public int totalTiendas() {
        return sedeDao.contarPorTipo("TIENDA");
    }

    public int totalInactivas() {
        return sedeDao.contarInactivas();
    }
    public Map<String, Object> obtenerHorariosSede(Integer idSede) {

        if (idSede == null) {
            throw new RuntimeException("Sede no válida.");
        }

        Sede sede = buscarPorId(idSede);

        if (sede == null) {
            throw new RuntimeException("La sede seleccionada no existe.");
        }

        Map<String, Object> respuesta = new HashMap<>();

        String descripcionPublica = sedeDao.obtenerDescripcionPublicaAtencion(idSede)
                .orElse("");

        List<Map<String, Object>> bloques = sedeDao.listarHorariosRecojoPorSede(idSede);

        respuesta.put("idSede", idSede);
        respuesta.put("descripcionPublica", descripcionPublica);
        respuesta.put("bloques", bloques);

        return respuesta;
    }

    public void guardarHorariosSede(Integer idSede,
                                    String descripcionPublica,
                                    String nombreBloque1,
                                    String horaInicio1,
                                    String horaFin1,
                                    String nombreBloque2,
                                    String horaInicio2,
                                    String horaFin2,
                                    Integer duracionFranjaMinutos,
                                    Integer capacidadPorFranja,
                                    boolean activo) {

        if (idSede == null) {
            throw new RuntimeException("Debe seleccionar una sede válida.");
        }

        Sede sede = buscarPorId(idSede);

        if (sede == null) {
            throw new RuntimeException("La sede seleccionada no existe.");
        }

        if (duracionFranjaMinutos == null || duracionFranjaMinutos <= 0) {
            throw new RuntimeException("La duración de la franja debe ser mayor a cero.");
        }

        if (capacidadPorFranja == null || capacidadPorFranja <= 0) {
            throw new RuntimeException("La capacidad por franja debe ser mayor a cero.");
        }

        if (capacidadPorFranja > 20) {
            throw new RuntimeException("La capacidad por franja no puede ser mayor a 20.");
        }

        descripcionPublica = descripcionPublica == null ? "" : descripcionPublica.trim();

        if (descripcionPublica.length() > 255) {
            throw new RuntimeException("La descripción pública no puede superar los 255 caracteres.");
        }

        if (horaInicio1 == null || horaInicio1.isBlank()
                || horaFin1 == null || horaFin1.isBlank()) {
            throw new RuntimeException("Debe registrar el primer bloque de atención.");
        }

        List<Map<String, Object>> bloques = new ArrayList<>();

        LocalTime inicio1 = parseHora(horaInicio1, "Hora inicio del bloque 1 no válida.");
        LocalTime fin1 = parseHora(horaFin1, "Hora fin del bloque 1 no válida.");

        if (!inicio1.isBefore(fin1)) {
            throw new RuntimeException("La hora de inicio del bloque 1 debe ser menor que la hora fin.");
        }

        bloques.add(crearBloque(
                nombreBloque1,
                inicio1,
                fin1,
                duracionFranjaMinutos,
                capacidadPorFranja,
                activo
        ));

        boolean existeBloque2 = horaInicio2 != null && !horaInicio2.isBlank()
                && horaFin2 != null && !horaFin2.isBlank();

        boolean bloque2Incompleto = (horaInicio2 != null && !horaInicio2.isBlank())
                || (horaFin2 != null && !horaFin2.isBlank());

        if (bloque2Incompleto && !existeBloque2) {
            throw new RuntimeException("Si registra el bloque 2, debe completar inicio y fin.");
        }

        if (existeBloque2) {
            LocalTime inicio2 = parseHora(horaInicio2, "Hora inicio del bloque 2 no válida.");
            LocalTime fin2 = parseHora(horaFin2, "Hora fin del bloque 2 no válida.");

            if (!inicio2.isBefore(fin2)) {
                throw new RuntimeException("La hora de inicio del bloque 2 debe ser menor que la hora fin.");
            }

            if (inicio2.isBefore(fin1)) {
                throw new RuntimeException("El bloque 2 no debe cruzarse con el bloque 1.");
            }

            bloques.add(crearBloque(
                    nombreBloque2,
                    inicio2,
                    fin2,
                    duracionFranjaMinutos,
                    capacidadPorFranja,
                    activo
            ));
        }

        sedeDao.guardarDescripcionPublicaAtencion(idSede, descripcionPublica, activo);

        sedeDao.eliminarHorariosRecojoPorSede(idSede);

        for (Map<String, Object> bloque : bloques) {
            sedeDao.crearHorarioRecojo(
                    idSede,
                    (String) bloque.get("nombreBloque"),
                    (LocalTime) bloque.get("horaInicio"),
                    (LocalTime) bloque.get("horaFin"),
                    (Integer) bloque.get("duracionFranjaMinutos"),
                    (Integer) bloque.get("capacidadPorFranja"),
                    (Boolean) bloque.get("activo")
            );
        }
    }

    private LocalTime parseHora(String valor, String mensajeError) {
        try {
            return LocalTime.parse(valor);
        } catch (DateTimeParseException e) {
            throw new RuntimeException(mensajeError);
        }
    }

    private Map<String, Object> crearBloque(String nombreBloque,
                                            LocalTime horaInicio,
                                            LocalTime horaFin,
                                            Integer duracionFranjaMinutos,
                                            Integer capacidadPorFranja,
                                            boolean activo) {

        Map<String, Object> bloque = new HashMap<>();

        String nombre = nombreBloque == null || nombreBloque.isBlank()
                ? "Bloque"
                : nombreBloque.trim();

        if (nombre.length() > 50) {
            nombre = nombre.substring(0, 50);
        }

        bloque.put("nombreBloque", nombre);
        bloque.put("horaInicio", horaInicio);
        bloque.put("horaFin", horaFin);
        bloque.put("duracionFranjaMinutos", duracionFranjaMinutos);
        bloque.put("capacidadPorFranja", capacidadPorFranja);
        bloque.put("activo", activo);

        return bloque;
    }

    public List<Map<String, Object>> listarHorariosDisponiblesConCupos(Integer idSede, LocalDate fecha) {

        if (idSede == null || fecha == null) {
            return List.of();
        }

        LocalDate hoy = LocalDate.now(ZONA_PERU);
        LocalTime horaActual = LocalTime.now(ZONA_PERU).withSecond(0).withNano(0);


        if (fecha.isBefore(hoy)) {
            return List.of();
        }

        Sede sede = buscarPorId(idSede);

        if (sede == null || !sede.isActivo()) {
            return List.of();
        }

        List<Map<String, Object>> bloques = sedeDao.listarHorariosRecojoPorSede(idSede);

        if (bloques == null || bloques.isEmpty()) {
            return List.of();
        }

        Map<LocalTime, Integer> reservas = sedeDao.contarReservasRecojoPorSedeYFecha(idSede, fecha);

        List<Map<String, Object>> horariosDisponibles = new ArrayList<>();

        for (Map<String, Object> bloque : bloques) {

            if (!valorActivo(bloque.get("activo"))) {
                continue;
            }

            LocalTime horaInicio = convertirHora(bloque.get("horaInicio"));
            LocalTime horaFin = convertirHora(bloque.get("horaFin"));

            Integer duracion = convertirEntero(bloque.get("duracionFranjaMinutos"), 20);
            Integer capacidad = convertirEntero(bloque.get("capacidadPorFranja"), 3);

            if (horaInicio == null || horaFin == null || !horaInicio.isBefore(horaFin)) {
                continue;
            }

            if (duracion <= 0 || capacidad <= 0) {
                continue;
            }

            LocalTime hora = horaInicio;

            while (hora.isBefore(horaFin)) {

                /*
                 * Si la fecha seleccionada es hoy, no mostramos horarios que ya pasaron.
                 * Ejemplo: si son las 19:00, no debe aparecer 10:00, 11:00, 18:40, etc.
                 */
                if (fecha.isEqual(hoy) && !hora.isAfter(horaActual)) {
                    hora = hora.plusMinutes(duracion);
                    continue;
                }

                Integer ocupados = reservas.getOrDefault(hora, 0);
                int cuposDisponibles = capacidad - ocupados;

                /*
                 * Si ya llegó al máximo de cupos, no se muestra el botón.
                 * Ejemplo: capacidad 3, ocupados 3 => desaparece.
                 */
                if (cuposDisponibles > 0) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("hora", hora.toString());
                    item.put("horaTexto", hora.toString());
                    item.put("ocupados", ocupados);
                    item.put("capacidad", capacidad);
                    item.put("cuposDisponibles", cuposDisponibles);
                    item.put("disponible", true);

                    horariosDisponibles.add(item);
                }

                hora = hora.plusMinutes(duracion);
            }
        }

        return horariosDisponibles;
    }

    public void validarHorarioRecojoDisponible(Integer idSede, LocalDate fecha, LocalTime hora) {

        if (idSede == null) {
            throw new RuntimeException("Debe seleccionar una sede válida.");
        }

        if (fecha == null) {
            throw new RuntimeException("Debe seleccionar una fecha de recojo.");
        }

        if (hora == null) {
            throw new RuntimeException("Debe seleccionar una hora de recojo.");
        }

        LocalDate hoy = LocalDate.now(ZONA_PERU);
        LocalTime horaActual = LocalTime.now(ZONA_PERU).withSecond(0).withNano(0);

        if (fecha.isBefore(hoy)) {
            throw new RuntimeException("No puede seleccionar una fecha pasada.");
        }

        LocalTime horaValidar = hora.withSecond(0).withNano(0);

        if (fecha.isEqual(hoy) && !horaValidar.isAfter(horaActual)) {
            throw new RuntimeException("No puede seleccionar un horario que ya pasó. Elija una hora disponible posterior a la actual.");
        }

        List<Map<String, Object>> horarios = listarHorariosDisponiblesConCupos(idSede, fecha);

        boolean existeDisponible = horarios.stream()
                .anyMatch(h -> horaValidar.toString().equals(String.valueOf(h.get("hora"))));

        if (!existeDisponible) {
            throw new RuntimeException("El horario seleccionado no está disponible o ya no tiene cupos.");
        }
    }


    private LocalTime convertirHora(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof LocalTime hora) {
            return hora;
        }

        if (valor instanceof java.sql.Time horaSql) {
            return horaSql.toLocalTime();
        }

        return LocalTime.parse(valor.toString().substring(0, 5));
    }

    private Integer convertirEntero(Object valor, Integer defecto) {

        if (valor == null) {
            return defecto;
        }

        if (valor instanceof Number numero) {
            return numero.intValue();
        }

        try {
            return Integer.parseInt(valor.toString());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private boolean valorActivo(Object valor) {

        if (valor == null) {
            return false;
        }

        if (valor instanceof Boolean b) {
            return b;
        }

        if (valor instanceof Number n) {
            return n.intValue() == 1;
        }

        return "true".equalsIgnoreCase(valor.toString()) || "1".equals(valor.toString());
    }

    public String generarCodigo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "";
        }

        return sedeDao.generarCodigo(tipo);
    }
}
