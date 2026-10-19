package com.codice.sra.services;

import com.codice.sra.models.EstadoBeneficioEstudiante;
import com.codice.sra.repositories.EstadoBeneficioEstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoBeneficioEstudianteService {

    @Autowired
    private EstadoBeneficioEstudianteRepository repository;

    public List<EstadoBeneficioEstudiante> obtenerActivos() {
        return repository.findByActivoTrue();
    }

    public List<EstadoBeneficioEstudiante> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoBeneficioEstudiante guardar(EstadoBeneficioEstudiante estado) {
        estado.setIdEstadoBeneficioEstudiante(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoBeneficioEstudiante darDeBaja(Long id) {
        EstadoBeneficioEstudiante estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}