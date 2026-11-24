package com.codice.sra.repositories;

import com.codice.sra.dtos.MateriaListadoDTO;
import com.codice.sra.dtos.MateriaPensumResponseDTO;
import com.codice.sra.models.Materia;
import com.codice.sra.models.Pensum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, Long> {

    Optional<Materia> findByCodigoMateriaIgnoreCase(String codigoMateria);

    boolean existsByCodigoMateriaIgnoreCase(String codigoMateria);

    boolean existsByNombreMateriaIgnoreCase(String nombreMateria);

    boolean existsByCodigoMateriaIgnoreCaseAndIdMateriaNot(String codigoMateria, Long idMateria);

    boolean existsByNombreMateriaIgnoreCaseAndIdMateriaNot(String nombreMateria, Long idMateria);

    @Query("""
            SELECT m.codigoMateria
            FROM Materia m
            JOIN m.prerrequisitos requisito
            WHERE requisito.idMateria = :idMateria AND m.estadoMateria = true
            ORDER BY m.codigoMateria
            """)
    List<String> findCodigosDependientesActivos(@Param("idMateria") Long idMateria);

    @Query("""
            SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END
            FROM PensumMateria pm
            WHERE pm.pensum.idPensum = :idPensum AND pm.materia.idMateria = :idMateria
            """)
    boolean existeAsignacion(@Param("idPensum") Long idPensum, @Param("idMateria") Long idMateria);

    /**
     * Obtiene las materias del pensum vigente o activo de la carrera,
     * ordenadas por ciclo recomendado y nombre de materia.
     */
    @Query("SELECT DISTINCT new com.codice.sra.dtos.MateriaPensumResponseDTO(" +
            "m.idMateria, m.codigoMateria, m.nombreMateria, pm.cicloRecomendado) " +
            "FROM PensumMateria pm " +
            "JOIN pm.materia m " +
            "JOIN pm.pensum p " +
            "WHERE p.carrera.idCarrera = :idCarrera " +
            "AND UPPER(TRIM(p.estadoPensum.estadoPensum)) IN ('VIGENTE', 'ACTIVO') " +
            "AND m.estadoMateria = true " +
            "ORDER BY pm.cicloRecomendado ASC, m.nombreMateria ASC")
    List<MateriaPensumResponseDTO> findMateriasActivasPorCarrera(@Param("idCarrera") Long idCarrera);

    @Query("""
        SELECT DISTINCT c.nombreCarrera
        FROM PensumMateria pm
        JOIN pm.pensum p
        JOIN p.carrera c
        JOIN p.estadoPensum ep
        WHERE pm.materia.idMateria = :idMateria
          AND UPPER(TRIM(ep.estadoPensum)) = 'VIGENTE'
    """)
    List<String> findCarrerasVigentesPorMateria(@Param("idMateria") Long idMateria);

    @Query("SELECT m FROM Materia m WHERE m.estadoMateria = true ORDER BY m.nombreMateria ASC")
    List<Materia> findTodasActivas();

    @Query("SELECT DISTINCT m FROM Materia m LEFT JOIN FETCH m.prerrequisitos WHERE m.idMateria = :idMateria")
    Optional<Materia> findByIdConPrerrequisitos(@Param("idMateria") Long idMateria);

    @Query(
            value = """
                    SELECT new com.codice.sra.dtos.MateriaListadoDTO(
                        m.idMateria, m.codigoMateria, m.nombreMateria,
                        m.unidadesValorativas, m.estadoMateria, SIZE(m.prerrequisitos)
                    )
                    FROM Materia m
                    WHERE (:estado IS NULL OR m.estadoMateria = :estado)
                      AND (
                          :busqueda = ''
                          OR LOCATE(:busqueda, CAST(FUNCTION('translate', LOWER(m.nombreMateria),
                              'áéíóúüñ', 'aeiouun') AS String)) > 0
                          OR LOCATE(:busqueda, CAST(FUNCTION('translate', LOWER(m.codigoMateria),
                              'áéíóúüñ', 'aeiouun') AS String)) > 0
                          OR (:codigoBusqueda <> '' AND LOCATE(:codigoBusqueda,
                              CAST(FUNCTION('translate', LOWER(m.codigoMateria),
                                  'áéíóúüñ -_.', 'aeiouun') AS String)) > 0)
                      )
                    ORDER BY m.nombreMateria, m.idMateria
                    """,
            countQuery = """
                    SELECT COUNT(m)
                    FROM Materia m
                    WHERE (:estado IS NULL OR m.estadoMateria = :estado)
                      AND (
                          :busqueda = ''
                          OR LOCATE(:busqueda, CAST(FUNCTION('translate', LOWER(m.nombreMateria),
                              'áéíóúüñ', 'aeiouun') AS String)) > 0
                          OR LOCATE(:busqueda, CAST(FUNCTION('translate', LOWER(m.codigoMateria),
                              'áéíóúüñ', 'aeiouun') AS String)) > 0
                          OR (:codigoBusqueda <> '' AND LOCATE(:codigoBusqueda,
                              CAST(FUNCTION('translate', LOWER(m.codigoMateria),
                                  'áéíóúüñ -_.', 'aeiouun') AS String)) > 0)
                      )
                    """
    )
    Page<MateriaListadoDTO> buscarParaGestion(
            @Param("busqueda") String busqueda,
            @Param("codigoBusqueda") String codigoBusqueda,
            @Param("estado") Boolean estado,
            Pageable pageable);

    @Query("""
            SELECT p
            FROM PensumMateria pm
            JOIN pm.pensum p
            JOIN FETCH p.carrera c
            JOIN FETCH p.estadoPensum
            WHERE pm.materia.idMateria = :idMateria
            ORDER BY c.nombreCarrera, p.version DESC, p.idPensum
            """)
    List<Pensum> findPensumsPorMateria(@Param("idMateria") Long idMateria);
}
