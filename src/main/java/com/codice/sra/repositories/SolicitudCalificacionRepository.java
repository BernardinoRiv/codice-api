package com.codice.sra.repositories;

import com.codice.sra.models.SolicitudCalificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudCalificacionRepository extends JpaRepository<SolicitudCalificacion, Long> {
    boolean existsByCalificacion_IdCalificacionAndEstadoSolicitud_EstadoSolicitudIgnoreCase(
            Long idCalificacion, String estado);

    List<SolicitudCalificacion> findByEstadoSolicitud_EstadoSolicitudIgnoreCase(String estado);

    List<SolicitudCalificacion> findByCalificacion_IdCalificacionOrderByFechaSolicitudDesc(Long idCalificacion);

    List<SolicitudCalificacion> findByCalificacion_IdCalificacionAndEstadoSolicitud_EstadoSolicitudIgnoreCaseOrderByFechaRespuestaDesc(
            Long idCalificacion, String estado);
}
