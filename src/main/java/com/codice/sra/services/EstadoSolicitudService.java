package com.codice.sra.services;

import com.codice.sra.models.EstadoSolicitud;
import com.codice.sra.repositories.EstadoSolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoSolicitudService {
    @Autowired
    private EstadoSolicitudRepository repository;

    public List<EstadoSolicitud> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoSolicitud guardar(EstadoSolicitud estado) {
        estado.setIdEstadoSolicitud(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoSolicitud darDeBaja(Long id) {
        EstadoSolicitud estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de solicitud no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}