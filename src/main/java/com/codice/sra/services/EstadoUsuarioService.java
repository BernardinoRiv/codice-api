package com.codice.sra.services;

import com.codice.sra.models.EstadoUsuario;
import com.codice.sra.repositories.EstadoUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoUsuarioService {
    @Autowired
    private EstadoUsuarioRepository repository;

    public List<EstadoUsuario> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoUsuario guardar(EstadoUsuario estado) {
        estado.setIdEstadoUsuario(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoUsuario darDeBaja(Long id) {
        EstadoUsuario estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de usuario no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}