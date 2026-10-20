package com.codice.sra.services;

import com.codice.sra.models.EstadoCiclo;
import com.codice.sra.repositories.EstadoCicloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EstadoCicloService {

    @Autowired
    private EstadoCicloRepository repository;

    public List<EstadoCiclo> obtenerTodos() {
        return repository.findAll(); // Mantenimiento: Activos e inactivos
    }

    public EstadoCiclo guardar(EstadoCiclo estado) {
        estado.setIdEstadoCiclo(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoCiclo cambiarEstado(Long id, boolean activo) {
        EstadoCiclo estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de ciclo no encontrado"));
        estado.setActivo(activo);
        return repository.save(estado);
    }
}