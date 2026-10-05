package com.codice.sra.repositories;

import com.codice.sra.models.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarreraRepository extends JpaRepository<Carrera, Long> {

//    @Query("SELECT DISTINCT c FROM Carrera c " +
//            "JOIN FETCH c.facultad f " +
//            "JOIN FETCH c.nivel n " +
//            "JOIN FETCH c.estadoCarrera ec " +
//            "JOIN c.carrerasSedes cs " +
//            "JOIN c.pensums p " +
//            "JOIN p.pensumMaterias pm " +
//            "JOIN pm.materia m " +
//            "WHERE cs.sede.idSede = :idSede " +
//            "AND UPPER(TRIM(ec.estadoCarrera)) = 'ACTIVO' " +
//            "AND UPPER(TRIM(p.estadoPensum.estadoPensum)) IN ('VIGENTE', 'ACTIVO') " +
//            "AND m.estadoMateria = true " +
//            "ORDER BY c.nombreCarrera ASC")
//    List<Carrera> findCarrerasOfertablesPorSede(@Param("idSede") Long idSede);


    @Query("SELECT DISTINCT c FROM Carrera c " +
            "JOIN FETCH c.facultad f " +
            "JOIN FETCH c.nivel n " +
            "JOIN FETCH c.estadoCarrera ec " +
            "JOIN c.pensums p " +
            "WHERE UPPER(TRIM(ec.estadoCarrera)) = 'ACTIVO' " +
            "AND UPPER(TRIM(p.estadoPensum.estadoPensum)) IN ('VIGENTE', 'ACTIVO') " +
            "ORDER BY c.nombreCarrera ASC")
    List<Carrera> findTodasCarrerasOfertables();


    Optional<Carrera> findByCodigoCarreraIgnoreCase(String codigoCarrera);


    @Query("SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END " +
            "FROM CarreraSede cs " +
            "WHERE cs.carrera.idCarrera = :idCarrera " +
            "AND cs.sede.idSede = :idSede")
    boolean existsByCarreraAndSede(@Param("idCarrera") Long idCarrera, @Param("idSede") Long idSede);


    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Pensum p " +
            "WHERE p.carrera.idCarrera = :idCarrera " +
            "AND UPPER(TRIM(p.estadoPensum.estadoPensum)) IN ('VIGENTE', 'ACTIVO')")
    boolean tienePensumVigente(@Param("idCarrera") Long idCarrera);

    @Query(value = """
        SELECT DISTINCT c.* 
        FROM carreras c
        INNER JOIN pensum p ON p.id_carrera = c.id_carrera
        INNER JOIN estados_pensum ep ON ep.id_estado_pensum = p.id_estado_pensum
        WHERE UPPER(TRIM(ep.estado_pensum)) = 'VIGENTE'
        ORDER BY c.nombre_carrera ASC
    """, nativeQuery = true)
    List<Carrera> findAllCarrerasConPensumVigente();

    @Query(value = """
        SELECT DISTINCT c.* 
        FROM carreras c
        INNER JOIN carreras_sedes cs ON cs.id_carrera = c.id_carrera
        INNER JOIN pensum p ON p.id_carrera = c.id_carrera
        INNER JOIN estados_pensum ep ON ep.id_estado_pensum = p.id_estado_pensum
        WHERE cs.id_sede = :idSede
          AND UPPER(TRIM(ep.estado_pensum)) = 'VIGENTE'
        ORDER BY c.nombre_carrera ASC
    """, nativeQuery = true)
    List<Carrera> findCarrerasOfertablesPorSede(@Param("idSede") Long idSede);
}