package com.vialsa.almacen.config;
import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
@Configuration
public class StartupConfig {
  @Bean CommandLineRunner initAdmin(UsuarioDao usuarioDao){
    return args -> usuarioDao.ensureAdminUserExists("admin","admin123");

  }
}
