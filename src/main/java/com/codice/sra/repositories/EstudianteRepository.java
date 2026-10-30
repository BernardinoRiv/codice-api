package com.codice.sra.repositories;

import com.codice.sra.models.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    // Busca al estudiante por el ID de su usuario asociado
    Optional<Estudiante> findByUsuarioIdUsuario(Long idUsuario);

    List<Estudiante> findByEstadoEstudiante_EstadoEstudiante(String estado);

    Optional<Estudiante> findByCarnet(String carnet);
    List<Estudiante> findByEstadoEstudiante_EstadoEstudianteIn(List<String> estados);

    Optional<Estudiante> findByPersona_NumeroDocumento(String numeroDocumento);




    // Valida si la persona ya es un estudiante activo con usuario asignado
    Optional<Estudiante> findByPersona_NumeroDocumentoAndUsuarioIsNotNull(String numeroDocumento);

    boolean existsByCarnet(String carnet);

    // Identifica aspirantes tentativos que superaron la fecha límite sin pagar
    @Query("SELECT e FROM Estudiante e " +
            "JOIN Matricula m ON m.estudiante.idEstudiante = e.idEstudiante " +
            "WHERE e.usuario IS NULL " +
            "AND m.fechaMatricula < :limiteTiempo")
    List<Estudiante> findEstudiantesTentativosExpirados(@Param("limiteTiempo") LocalDateTime limiteTiempo);

    @Query("SELECT COALESCE(MAX(e.idEstudiante), 0) FROM Estudiante e")
    Long obtenerUltimoIdEstudiante();


    // Método requerido para resolver el expediente a partir de la entidad Persona
    Optional<Estudiante> findByPersona_IdPersona(Long idPersona);
}