package com.codice.sra.services;

import com.codice.sra.models.EstadoPensum;
import com.codice.sra.repositories.EstadoPensumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoPensumService {
    @Autowired
    private EstadoPensumRepository repository;

    public List<EstadoPensum> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoPensum guardar(EstadoPensum estado) {
        estado.setIdEstadoPensum(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoPensum darDeBaja(Long id) {
        EstadoPensum estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de pensum no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}