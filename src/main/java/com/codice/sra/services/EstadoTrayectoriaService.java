package com.codice.sra.services;

import com.codice.sra.models.EstadoTrayectoria;
import com.codice.sra.repositories.EstadoTrayectoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoTrayectoriaService {
    @Autowired
    private EstadoTrayectoriaRepository repository;

    public List<EstadoTrayectoria> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoTrayectoria guardar(EstadoTrayectoria estado) {
        estado.setIdEstadoTrayectoria(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoTrayectoria darDeBaja(Long id) {
        EstadoTrayectoria estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de trayectoria no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}