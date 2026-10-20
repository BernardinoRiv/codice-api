package com.codice.sra.services;

import com.codice.sra.models.EstadoCarrera;
import com.codice.sra.repositories.EstadoCarreraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EstadoCarreraService {

    @Autowired
    private EstadoCarreraRepository repository;

    public List<EstadoCarrera> obtenerTodos() {
        return repository.findAll(); // Vista de mantenimiento (activos e inactivos)
    }

    public EstadoCarrera guardar(EstadoCarrera estado) {
        estado.setIdEstadoCarrera(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoCarrera cambiarEstado(Long id, boolean activo) {
        EstadoCarrera estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de carrera no encontrado"));
        estado.setActivo(activo);
        return repository.save(estado);
    }
}