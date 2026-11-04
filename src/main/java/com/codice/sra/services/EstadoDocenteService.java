package com.codice.sra.services;

import com.codice.sra.models.EstadoDocente;
import com.codice.sra.repositories.EstadoDocenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EstadoDocenteService {

    @Autowired
    private EstadoDocenteRepository repository;

    public List<EstadoDocente> obtenerTodos() {
        return repository.findAll(); // Mantenimiento: activos e inactivos
    }

    public EstadoDocente guardar(EstadoDocente estado) {
        estado.setIdEstadoDocente(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoDocente cambiarEstado(Long id, boolean activo) {
        EstadoDocente estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de docente no encontrado"));
        estado.setActivo(activo);
        return repository.save(estado);
    }
}