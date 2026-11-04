package com.codice.sra.services;

import com.codice.sra.models.EstadoEdificio;
import com.codice.sra.repositories.EstadoEdificioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EstadoEdificioService {

    @Autowired
    private EstadoEdificioRepository repository;

    public List<EstadoEdificio> obtenerTodos() {
        return repository.findAll(); // Vista de mantenimiento (activos e inactivos)
    }

    public List<EstadoEdificio> obtenerActivos() {
        return repository.findByActivoTrue(); // Vista operativa (solo vigentes)
    }

    public Optional<EstadoEdificio> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    public EstadoEdificio guardar(EstadoEdificio estado) {
        estado.setIdEstadoEdificio(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoEdificio cambiarEstado(Long id, boolean activo) {
        EstadoEdificio estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de edificio no encontrado"));
        estado.setActivo(activo);
        return repository.save(estado);
    }
}