package com.codice.sra.services;

import com.codice.sra.models.EstadoFacultad;
import com.codice.sra.repositories.EstadoFacultadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoFacultadService {
    @Autowired
    private EstadoFacultadRepository repository;

    public List<EstadoFacultad> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoFacultad guardar(EstadoFacultad estado) {
        estado.setIdEstadoFacultad(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoFacultad darDeBaja(Long id) {
        EstadoFacultad estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de facultad no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}