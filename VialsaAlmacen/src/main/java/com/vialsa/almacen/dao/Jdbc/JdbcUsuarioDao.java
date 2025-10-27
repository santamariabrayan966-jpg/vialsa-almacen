package com.vialsa.almacen.dao.Jdbc;
import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import com.vialsa.almacen.model.Usuario;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import java.util.List; import java.util.Optional;
@Repository
public class JdbcUsuarioDao implements UsuarioDao {
  private final JdbcTemplate jdbc; private final PasswordEncoder encoder;
  public JdbcUsuarioDao(JdbcTemplate jdbc, PasswordEncoder encoder){ this.jdbc=jdbc; this.encoder=encoder; }
  @Override public Optional<Usuario> findByNombreUsuario(String username){
    List<Usuario> l = jdbc.query("SELECT idUsuario, NombreUsuario AS nombreUsuario, Contrasena AS contrasena, idRol FROM usuarios WHERE NombreUsuario=?",
      new BeanPropertyRowMapper<>(Usuario.class), username);
    return l.stream().findFirst();
  }
  @Override public void ensureAdminUserExists(String username, String rawPassword){
    Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE NombreUsuario=?", Integer.class, username);
    if(c!=null && c==0){
      String hash = encoder.encode(rawPassword);
      jdbc.update("INSERT IGNORE INTO roles(idRol, NombreRol, descripcion) VALUES (1,'ADMIN','Administrador')");
      jdbc.update("INSERT IGNORE INTO estadousuario(idEstadoUsuario, NombreEstado) VALUES (1,'ACTIVO')");
      jdbc.update("INSERT INTO usuarios(NombreUsuario, Contrasena, idRol, idEstadoUsuario) VALUES (?,?,1,1)", username, hash);
    }
  }
}
