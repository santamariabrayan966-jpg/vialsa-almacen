package com.dentia.service.agenda;

import com.dentia.dao.interfaces.ICatalogoAgendaDao;
import com.dentia.dao.interfaces.IProfesionalAgendaDao;
import com.dentia.model.agenda.BloqueoAgenda;
import com.dentia.model.agenda.Especialidad;
import com.dentia.model.agenda.HorarioProfesional;
import com.dentia.model.agenda.Profesional;
import com.dentia.model.agenda.TipoAtencion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ProfesionalAgendaService {
    private static final List<String> MOTIVOS_BLOQUEO = List.of(
            "VACACIONES", "PERMISO", "DESCANSO", "CAPACITACION", "EMERGENCIA", "OTRO");

    private final IProfesionalAgendaDao profesionalDao;
    private final ICatalogoAgendaDao catalogoDao;

    public ProfesionalAgendaService(IProfesionalAgendaDao profesionalDao, ICatalogoAgendaDao catalogoDao) {
        this.profesionalDao = profesionalDao;
        this.catalogoDao = catalogoDao;
    }

    public List<Profesional> listarTodos() { return profesionalDao.listarTodos(); }
    public List<Profesional> buscarProfesionales(String texto, Long especialidad, Boolean activo) {
        String q=texto==null?"":texto.trim().toLowerCase(Locale.ROOT);
        return listarTodos().stream()
                .filter(p->activo==null||p.activo()==activo)
                .filter(p->especialidad==null||p.especialidades().stream().anyMatch(e->especialidad.equals(e.idEspecialidad())))
                .filter(p->q.isBlank()||(p.nombreCompleto()+" "+nvl(p.documento())).toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }
    public List<Profesional> listarActivos() { return profesionalDao.listarActivos(); }
    public Profesional buscar(long id) { return profesionalDao.buscarPorId(id)
            .orElseThrow(() -> new AgendaValidationException("El profesional no existe.")); }
    public List<Especialidad> listarEspecialidades(boolean soloActivas) { return catalogoDao.listarEspecialidades(soloActivas); }
    public List<TipoAtencion> listarTiposAtencion(boolean soloActivos) { return catalogoDao.listarTiposAtencion(soloActivos); }
    public List<String> motivosBloqueo() { return MOTIVOS_BLOQUEO; }

    @Transactional
    public long guardarProfesional(Profesional profesional, List<Long> especialidades) {
        validarProfesional(profesional);
        if (profesional.idUsuario() != null && listarTodos().stream().anyMatch(p ->
                profesional.idUsuario().equals(p.idUsuario()) && !java.util.Objects.equals(profesional.idProfesional(), p.idProfesional())))
            throw new AgendaValidationException("La cuenta interna ya esta vinculada a otro profesional.");
        long id;
        if (profesional.idProfesional() == null) id = profesionalDao.crear(profesional);
        else { profesionalDao.actualizar(profesional); id = profesional.idProfesional(); }
        profesionalDao.reemplazarEspecialidades(id, especialidades == null ? List.of() : especialidades);
        return id;
    }

    public void cambiarEstado(long id, boolean activo) {
        buscar(id);
        profesionalDao.cambiarEstado(id, activo);
    }

    public List<HorarioProfesional> listarHorarios(long idProfesional) { return profesionalDao.listarHorarios(idProfesional); }

    @Transactional
    public long agregarHorario(long idProfesional, int dia, java.time.LocalTime inicio, java.time.LocalTime fin) {
        return guardarHorario(null,idProfesional,dia,inicio,fin);
    }

    @Transactional
    public long guardarHorario(Long idHorario,long idProfesional,int dia,java.time.LocalTime inicio,java.time.LocalTime fin) {
        buscar(idProfesional);
        if (dia < 1 || dia > 7) throw new AgendaValidationException("El dia de la semana no es valido.");
        if (inicio == null || fin == null || !inicio.isBefore(fin))
            throw new AgendaValidationException("El horario debe tener un rango valido.");
        boolean solapa = profesionalDao.listarHorariosActivos(idProfesional, dia).stream()
                .filter(h->idHorario==null||!idHorario.equals(h.idHorario()))
                .anyMatch(h -> h.horaInicio().isBefore(fin) && h.horaFin().isAfter(inicio));
        if (solapa) throw new AgendaValidationException("El turno se cruza con otro bloque del mismo día. Deja una pausa entre turnos, por ejemplo 09:00–12:00 y 17:00–20:00.");
        HorarioProfesional h=new HorarioProfesional(idHorario,idProfesional,dia,inicio,fin,true);
        if(idHorario==null)return profesionalDao.agregarHorario(h);
        profesionalDao.actualizarHorario(h);return idHorario;
    }

    public void eliminarHorario(long idProfesional, long idHorario) {
        profesionalDao.eliminarHorario(idHorario, idProfesional);
    }

    public List<BloqueoAgenda> listarBloqueos(long idProfesional, LocalDateTime desde, LocalDateTime hasta) {
        return profesionalDao.listarBloqueos(idProfesional, desde, hasta);
    }

    @Transactional
    public long crearBloqueo(long idProfesional, LocalDateTime inicio, LocalDateTime fin,
                             String motivo, String observacion, int creadoPor) {
        buscar(idProfesional);
        String motivoNormalizado = motivo == null ? "" : motivo.trim().toUpperCase();
        if (!MOTIVOS_BLOQUEO.contains(motivoNormalizado))
            throw new AgendaValidationException("El motivo del bloqueo no es valido.");
        if (inicio == null || fin == null || !inicio.isBefore(fin))
            throw new AgendaValidationException("El bloqueo debe tener un rango valido.");
        return profesionalDao.crearBloqueo(new BloqueoAgenda(null, idProfesional, inicio, fin,
                motivoNormalizado, observacion, creadoPor, null));
    }

    public void eliminarBloqueo(long idProfesional, long idBloqueo) {
        profesionalDao.eliminarBloqueo(idBloqueo, idProfesional);
    }

    public long guardarTipoAtencion(TipoAtencion tipo) {
        if (tipo.nombre() == null || tipo.nombre().isBlank())
            throw new AgendaValidationException("El tipo de atencion necesita un nombre.");
        if (tipo.duracionMinutos() < 5 || tipo.duracionMinutos() > 480)
            throw new AgendaValidationException("La duracion debe estar entre 5 y 480 minutos.");
        boolean duplicado=listarTiposAtencion(false).stream().anyMatch(t->
                !java.util.Objects.equals(t.idTipoAtencion(),tipo.idTipoAtencion())&&t.nombre().equalsIgnoreCase(tipo.nombre().trim()));
        if(duplicado)throw new AgendaValidationException("Ya existe un tipo de atencion con ese nombre.");
        return catalogoDao.guardarTipoAtencion(tipo);
    }

    public long guardarEspecialidad(Especialidad especialidad) {
        if (especialidad.nombre() == null || especialidad.nombre().isBlank())
            throw new AgendaValidationException("La especialidad necesita un nombre.");
        boolean duplicado=listarEspecialidades(false).stream().anyMatch(e->
                !java.util.Objects.equals(e.idEspecialidad(),especialidad.idEspecialidad())&&e.nombre().equalsIgnoreCase(especialidad.nombre().trim()));
        if(duplicado)throw new AgendaValidationException("Ya existe una especialidad con ese nombre.");
        return catalogoDao.guardarEspecialidad(especialidad);
    }

    public void cambiarEstadoEspecialidad(long idEspecialidad, boolean activo) {
        catalogoDao.cambiarEstadoEspecialidad(idEspecialidad, activo);
    }

    public void cambiarEstadoTipoAtencion(long idTipoAtencion, boolean activo) {
        catalogoDao.cambiarEstadoTipoAtencion(idTipoAtencion, activo);
    }

    private void validarProfesional(Profesional p) {
        if (p == null || p.nombres() == null || p.nombres().isBlank()
                || p.apellidos() == null || p.apellidos().isBlank())
            throw new AgendaValidationException("Nombres y apellidos son obligatorios.");
        if (p.correo() != null && !p.correo().isBlank() && !p.correo().contains("@"))
            throw new AgendaValidationException("El correo del profesional no es valido.");
    }
    private String nvl(String value){return value==null?"":value;}
}
