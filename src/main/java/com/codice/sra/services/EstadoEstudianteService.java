package com.codice.sra.services;

import com.codice.sra.models.EstadoEstudiante;
import com.codice.sra.repositories.EstadoEstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoEstudianteService {
    @Autowired
    private EstadoEstudianteRepository repository;

    public List<EstadoEstudiante> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoEstudiante guardar(EstadoEstudiante estado) {
        estado.setIdEstadoEstudiante(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoEstudiante darDeBaja(Long id) {
        EstadoEstudiante estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de estudiante no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}