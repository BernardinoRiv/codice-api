package com.codice.sra.services;

import com.codice.sra.models.EstadoRecargo;
import com.codice.sra.repositories.EstadoRecargoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoRecargoService {
    @Autowired
    private EstadoRecargoRepository repository;

    public List<EstadoRecargo> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoRecargo guardar(EstadoRecargo estado) {
        estado.setIdEstadoRecargo(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoRecargo darDeBaja(Long id) {
        EstadoRecargo estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de recargo no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}