package com.codice.sra.services;

import com.codice.sra.models.EstadoCargo;
import com.codice.sra.repositories.EstadoCargoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EstadoCargoService {

    @Autowired
    private EstadoCargoRepository repository;

    public List<EstadoCargo> obtenerTodos() {
        return repository.findAll(); // Mantenimiento: Activos e inactivos
    }

    public EstadoCargo guardar(EstadoCargo estado) {
        estado.setIdEstadoCargo(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoCargo cambiarEstado(Long id, boolean activo) {
        EstadoCargo estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de cargo no encontrado"));
        estado.setActivo(activo);
        return repository.save(estado);
    }
}