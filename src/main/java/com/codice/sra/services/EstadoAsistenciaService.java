package com.codice.sra.services;

import com.codice.sra.models.EstadoAsistencia;
import com.codice.sra.repositories.EstadoAsistenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoAsistenciaService {

    @Autowired
    private EstadoAsistenciaRepository repository;

    public List<EstadoAsistencia> obtenerActivos() {
        return repository.findByActivoTrue();
    }

    // Vista de mantenimiento
    public List<EstadoAsistencia> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoAsistencia guardar(EstadoAsistencia estado) {
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoAsistencia darDeBaja(Long id) {
        EstadoAsistencia estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de asistencia no encontrado"));
        estado.setActivo(false); // Baja lógica
        return repository.save(estado);
    }
}