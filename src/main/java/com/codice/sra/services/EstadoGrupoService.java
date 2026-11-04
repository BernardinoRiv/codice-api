package com.codice.sra.services;

import com.codice.sra.models.EstadoGrupo;
import com.codice.sra.repositories.EstadoGrupoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoGrupoService {
    @Autowired
    private EstadoGrupoRepository repository;

    public List<EstadoGrupo> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoGrupo guardar(EstadoGrupo estado) {
        estado.setIdEstadoGrupo(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoGrupo darDeBaja(Long id) {
        EstadoGrupo estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de grupo no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}