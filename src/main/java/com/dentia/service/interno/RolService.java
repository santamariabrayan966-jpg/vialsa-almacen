package com.dentia.service.interno;

import com.dentia.dao.interfaces.RolDao;
import com.dentia.model.interno.Rol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.dentia.service.interno.RolValidationException.Codigo.*;

@Service
public class RolService {
    private final RolDao rolDao;

    public RolService(RolDao rolDao) { this.rolDao = rolDao; }

    public List<Rol> listarRoles() { return rolDao.listarRoles(); }
    public List<Rol> listarRolesInternos() { return rolDao.listarRolesInternos(); }
    public Rol obtenerPorId(int idRol) { return rolDao.obtenerPorId(idRol); }

    @Transactional
    public int crearRol(Rol rol) {
        normalizarYValidar(rol);
        if (existeNombre(rol.getNombreRol(), null))
            throw new RolValidationException(NOMBRE_DUPLICADO, "Ya existe un rol con ese nombre.");
        rol.setActivo(true);
        return rolDao.crearRol(rol);
    }

    @Transactional
    public void actualizarRol(Rol rol) {
        if (rol == null || rol.getIdRol() == null || rol.getIdRol() <= 0)
            throw new RolValidationException(NO_ENCONTRADO, "El rol no existe.");
        if (esAdmin(rol.getIdRol()))
            throw new RolValidationException(ADMIN_PROTEGIDO, "El rol ADMIN esta protegido.");
        normalizarYValidar(rol);
        if (existeNombre(rol.getNombreRol(), rol.getIdRol()))
            throw new RolValidationException(NOMBRE_DUPLICADO, "Ya existe un rol con ese nombre.");
        rolDao.actualizarRol(rol);
    }

    @Transactional
    public void eliminarRol(int idRol) {
        protegerAdmin(idRol);
        if (rolDao.contarUsuariosAsociados(idRol) > 0)
            throw new RolValidationException(USUARIOS_ASOCIADOS, "El rol tiene usuarios asociados.");
        rolDao.eliminarRol(idRol);
    }

    @Transactional
    public void cambiarEstadoActivo(int idRol, boolean activo) {
        protegerAdmin(idRol);
        rolDao.cambiarEstadoActivo(idRol, activo);
    }

    public boolean esAdmin(int idRol) {
        Rol rol = obtenerPorId(idRol);
        return rol != null && "ADMIN".equalsIgnoreCase(rol.getNombreRol());
    }

    private void protegerAdmin(int idRol) {
        if (esAdmin(idRol))
            throw new RolValidationException(ADMIN_PROTEGIDO, "El rol ADMIN esta protegido.");
    }

    private void normalizarYValidar(Rol rol) {
        if (rol == null || rol.getNombreRol() == null || rol.getNombreRol().isBlank())
            throw new RolValidationException(NOMBRE_REQUERIDO, "El nombre del rol es obligatorio.");
        rol.setNombreRol(rol.getNombreRol().trim());
        rol.setDescripcion(rol.getDescripcion() == null || rol.getDescripcion().isBlank()
                ? null : rol.getDescripcion().trim());
    }

    private boolean existeNombre(String nombre, Integer excluirId) {
        return listarRoles().stream().anyMatch(r -> r.getNombreRol() != null
                && r.getNombreRol().trim().equalsIgnoreCase(nombre)
                && (excluirId == null || !excluirId.equals(r.getIdRol())));
    }
}
