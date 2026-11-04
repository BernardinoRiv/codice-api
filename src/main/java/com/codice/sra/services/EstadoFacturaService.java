package com.codice.sra.services;

import com.codice.sra.models.EstadoFactura;
import com.codice.sra.repositories.EstadoFacturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoFacturaService {
    @Autowired
    private EstadoFacturaRepository repository;

    public List<EstadoFactura> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoFactura guardar(EstadoFactura estado) {
        estado.setIdEstadoFactura(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoFactura darDeBaja(Long id) {
        EstadoFactura estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de factura no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}