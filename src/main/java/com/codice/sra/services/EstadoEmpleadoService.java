package com.codice.sra.services;

import com.codice.sra.models.EstadoEmpleado;
import com.codice.sra.repositories.EstadoEmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoEmpleadoService {
    @Autowired
    private EstadoEmpleadoRepository repository;

    public List<EstadoEmpleado> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoEmpleado guardar(EstadoEmpleado estado) {
        estado.setIdEstadoEmpleado(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoEmpleado darDeBaja(Long id) {
        EstadoEmpleado estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de empleado no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}