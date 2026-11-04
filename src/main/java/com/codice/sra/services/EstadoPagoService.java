package com.codice.sra.services;

import com.codice.sra.models.EstadoPago;
import com.codice.sra.repositories.EstadoPagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EstadoPagoService {
    @Autowired
    private EstadoPagoRepository repository;

    public List<EstadoPago> obtenerTodos() {
        return repository.findAll();
    }

    public EstadoPago guardar(EstadoPago estado) {
        estado.setIdEstadoPago(null);
        estado.setActivo(true);
        return repository.save(estado);
    }

    public EstadoPago darDeBaja(Long id) {
        EstadoPago estado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de pago no encontrado"));
        estado.setActivo(false);
        return repository.save(estado);
    }
}