package com.codice.sra.services;

import com.codice.sra.models.EstadoBeneficio;
import com.codice.sra.repositories.EstadoBeneficioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoBeneficioService {

    @Autowired
    private EstadoBeneficioRepository repository;

    public List<EstadoBeneficio> obtenerActivos() {
        return repository.findByActivoTrue();
    }

    public EstadoBeneficio guardar(EstadoBeneficio estado) {
        estado.setIdEstadoBeneficio(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoBeneficio darDeBaja(Long id) {
        EstadoBeneficio estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de beneficio no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}