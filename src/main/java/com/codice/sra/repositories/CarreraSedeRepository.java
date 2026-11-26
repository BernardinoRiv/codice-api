package com.codice.sra.repositories;

import com.codice.sra.models.CarreraSede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CarreraSedeRepository extends JpaRepository<CarreraSede, Long> {

    @Query("""
            SELECT cs FROM CarreraSede cs
            JOIN FETCH cs.carrera c
            LEFT JOIN FETCH c.nivel
            WHERE cs.sede.idSede = :idSede
              AND UPPER(TRIM(c.estadoCarrera.estadoCarrera)) = 'ACTIVA'
              AND EXISTS (SELECT p.idPensum FROM Pensum p WHERE p.carrera = c AND p.estadoPensum.idEstadoPensum = 1)
            ORDER BY c.nombreCarrera
            """)
    List<CarreraSede> listarParaNuevoIngreso(@Param("idSede") Long idSede);


    @Query("SELECT cs FROM CarreraSede cs JOIN FETCH cs.sede WHERE cs.carrera.idCarrera = :idCarrera")
    List<CarreraSede> findByCarreraIdConSede(@Param("idCarrera") Long idCarrera);

    @Query(value = "SELECT COUNT(*) FROM estudiantes_carreras WHERE id_carrera_sede = :idCarreraSede", nativeQuery = true)
    long contarEstudiantesAsociados(@Param("idCarreraSede") Long idCarreraSede);

    @Query(value = """
            SELECT COUNT(DISTINCT g.id_grupo)
            FROM grupos g
            JOIN pensum_materias pm ON pm.id_materia = g.id_materia
            JOIN pensum p ON p.id_pensum = pm.id_pensum
            WHERE p.id_carrera = :idCarrera AND g.id_sede = :idSede
            """, nativeQuery = true)
    long contarGruposPotencialmenteRelacionados(@Param("idCarrera") Long idCarrera, @Param("idSede") Long idSede);
}
