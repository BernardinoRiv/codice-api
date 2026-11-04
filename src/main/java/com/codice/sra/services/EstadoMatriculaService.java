package com.codice.sra.services;

import com.codice.sra.models.EstadoMatricula;
import com.codice.sra.repositories.EstadoMatriculaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoMatriculaService {
    @Autowired
    private EstadoMatriculaRepository repository;

    public List<EstadoMatricula> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoMatricula guardar(EstadoMatricula estado) {
        estado.setIdEstadoMatricula(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoMatricula darDeBaja(Long id) {
        EstadoMatricula estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de matrícula no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}