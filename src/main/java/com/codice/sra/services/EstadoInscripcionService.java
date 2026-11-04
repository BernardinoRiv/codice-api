package com.codice.sra.services;

import com.codice.sra.models.EstadoInscripcion;
import com.codice.sra.repositories.EstadoInscripcionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoInscripcionService {
    @Autowired
    private EstadoInscripcionRepository repository;

    public List<EstadoInscripcion> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoInscripcion guardar(EstadoInscripcion estado) {
        estado.setIdEstadoInscripcion(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoInscripcion darDeBaja(Long id) {
        EstadoInscripcion estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de inscripción no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}