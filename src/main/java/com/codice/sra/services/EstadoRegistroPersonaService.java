package com.codice.sra.services;

import com.codice.sra.models.EstadoRegistroPersona;
import com.codice.sra.repositories.EstadoRegistroPersonaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoRegistroPersonaService {
    @Autowired
    private EstadoRegistroPersonaRepository repository;

    public List<EstadoRegistroPersona> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoRegistroPersona guardar(EstadoRegistroPersona estado) {
        estado.getIdEstadoRegistro();
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoRegistroPersona darDeBaja(Long id) {
        EstadoRegistroPersona estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de registro persona no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}