package com.codice.sra.repositories;

import com.codice.sra.models.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    boolean existsByReferencia(String referencia);

    Optional<Pago> findByReferencia(String referencia);

    Optional<Pago> findByIdPagoAndEstudiante_Usuario_IdUsuario(Long idPago, Long idUsuario);

    @Query("""
            SELECT new map(
                p.idPago as idPago,
                p.referencia as referencia,
                coalesce(f.numeroFactura, p.referencia) as numeroFactura,
                f.idFactura as idFactura,
                p.fechaPago as fechaPago,
                p.montoPagado as montoTotal,
                m.metodoPago as metodoPago,
                e.carnet as carnet,
                concat(concat(pe.nombres, ' '), pe.apellidos) as nombreEstudiante
            )
            FROM Pago p
            JOIN p.estudiante e
            JOIN e.persona pe
            JOIN p.metodoPago m
            LEFT JOIN Factura f ON f.pago = p
            WHERE p.fechaPago >= :inicio AND p.fechaPago < :finExclusivo
            ORDER BY p.fechaPago DESC, p.idPago DESC
            """)
    List<Map<String, Object>> listarHistorialPorRango(
            @Param("inicio") LocalDateTime inicio,
            @Param("finExclusivo") LocalDateTime finExclusivo);
}