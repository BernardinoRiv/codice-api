package com.codice.sra.repositories;

import com.codice.sra.models.SesionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Long> {
    List<SesionUsuario> findByUsuarioIdUsuarioOrderByFechaInicioDesc(Long idUsuario);

    List<SesionUsuario> findByUsuarioIdUsuarioAndFechaFinIsNull(Long idUsuario);
    @Query("SELECT DISTINCT s.direccionIp FROM SesionUsuario s WHERE s.usuario.idUsuario = :idUsuario AND s.exitosa = true")
    List<String> findDistinctIpsByUsuario(@Param("idUsuario") Long idUsuario);

    @Query("SELECT DISTINCT s.agenteUsuario FROM SesionUsuario s WHERE s.usuario.idUsuario = :idUsuario AND s.exitosa = true")
    List<String> findDistinctAgentesByUsuario(@Param("idUsuario") Long idUsuario);

    @Query("UPDATE SesionUsuario s SET s.fechaFin = CURRENT_TIMESTAMP WHERE s.usuario.idUsuario = :idUsuario AND s.fechaFin IS NULL")
    void cerrarSesionesActivas(@Param("idUsuario") Long idUsuario);

    boolean existsByUsuarioIdUsuarioAndFechaFinIsNull(Long idUsuario);

}