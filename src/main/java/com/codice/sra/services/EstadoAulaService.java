package com.codice.sra.services;

import com.codice.sra.models.EstadoAula;
import com.codice.sra.repositories.EstadoAulaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoAulaService {

    @Autowired
    private EstadoAulaRepository repository;

    public List<EstadoAula> obtenerActivos() {
        return repository.findByActivoTrue();
    }

    public List<EstadoAula> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoAula guardar(EstadoAula estado) {
        estado.setIdEstadoAula(null); // Evita conflictos con autoincrementable
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoAula darDeBaja(Long id) {
        EstadoAula estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de aula no encontrado"));
        estado.setActivo(false); // Baja lógica
        return repository.save(estado);
    }
}