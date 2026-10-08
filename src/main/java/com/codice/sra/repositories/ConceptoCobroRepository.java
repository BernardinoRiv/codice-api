package com.codice.sra.repositories;

import com.codice.sra.models.ConceptoCobro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConceptoCobroRepository extends JpaRepository<ConceptoCobro, Long> {

    Optional<ConceptoCobro> findByCiclo_IdCicloAndTipoCobro_TipoCobro(Long idCiclo, String tipoCobro);

    List<ConceptoCobro> findByCiclo_IdCicloAndTipoCobro_TipoCobroContainingIgnoreCase(Long idCiclo, String palabraClave);
    List<ConceptoCobro> findByCiclo_IdCiclo(Long idCiclo);
}