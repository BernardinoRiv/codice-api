package com.codice.sra.repositories;

import com.codice.sra.models.Ciclo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CicloRepository extends JpaRepository<Ciclo, Long> {

    Optional<Ciclo> findByCodigoCicloIgnoreCase(String codigoCiclo);

    // Listar ciclos activos para poblar el dropdown de contexto
    @Query("SELECT c FROM Ciclo c WHERE UPPER(c.estadoCiclo.estadoCiclo) = 'ACTIVO' ORDER BY c.anio DESC, c.numeroCiclo DESC")
    List<Ciclo> findCiclosActivos();

    // Consulta con join fetch del estado para evitar LazyInitializationException
    @Query("SELECT c FROM Ciclo c JOIN FETCH c.estadoCiclo WHERE c.idCiclo = :idCiclo")
    Optional<Ciclo> findByIdConEstado(@Param("idCiclo") Long idCiclo);
}