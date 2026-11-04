package com.codice.sra.services;

import com.codice.sra.models.EstadoMateriaEstudiante;
import com.codice.sra.repositories.EstadoMateriaEstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoMateriaEstudianteService {
    @Autowired
    private EstadoMateriaEstudianteRepository repository;

    public List<EstadoMateriaEstudiante> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoMateriaEstudiante guardar(EstadoMateriaEstudiante estado) {
        estado.setIdEstadoMateria(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoMateriaEstudiante darDeBaja(Long id) {
        EstadoMateriaEstudiante estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado materia estudiante no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}