package com.codice.sra.services;

import com.codice.sra.models.EstadoSede;
import com.codice.sra.repositories.EstadoSedeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoSedeService {
    @Autowired
    private EstadoSedeRepository repository;

    public List<EstadoSede> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoSede guardar(EstadoSede estado) {
        estado.setIdEstadoSede(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoSede darDeBaja(Long id) {
        EstadoSede estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de sede no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}