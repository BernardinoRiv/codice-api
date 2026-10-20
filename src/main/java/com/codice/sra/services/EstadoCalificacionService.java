package com.codice.sra.services;

import com.codice.sra.models.EstadoCalificacion;
import com.codice.sra.repositories.EstadoCalificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EstadoCalificacionService {

    @Autowired
    private EstadoCalificacionRepository repository;

    public List<EstadoCalificacion> obtenerTodos() {
        return repository.findAll(); // Vista de mantenimiento (todos)
    }

    public EstadoCalificacion guardar(EstadoCalificacion estado) {
        estado.setIdEstadoCalificacion(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoCalificacion cambiarEstado(Long id, boolean activo) {
        EstadoCalificacion estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de calificación no encontrado"));
        estado.setActivo(activo);
        return repository.save(estado);
    }

}