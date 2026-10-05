package com.codice.sra.services;

import com.codice.sra.dtos.CarreraResponseDTO;
import com.codice.sra.models.Carrera;
import com.codice.sra.repositories.CarreraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarreraService {

    private final CarreraRepository carreraRepository;
    @Transactional(readOnly = true)
    public List<CarreraResponseDTO> listarCarrerasOfertables(Long idSede) {
        log.info("Ejecutando resolución de carreras ofertables para sede ID: [{}]", idSede);

        List<Carrera> carreras = (idSede != null && idSede > 0)
                ? carreraRepository.findCarrerasOfertablesPorSede(idSede)
                : carreraRepository.findAllCarrerasConPensumVigente();

        log.info("Total de carreras recuperadas para sede [{}]: {}", idSede, carreras.size());

        return carreras.stream()
                .map(this::mapearADTO)
                .toList();
    }

    private CarreraResponseDTO mapearADTO(Carrera c) {
        String facultadNombre = (c.getFacultad() != null)
                ? c.getFacultad().getNombreFacultad()
                : "Facultad Institucional";

        return new CarreraResponseDTO(
                c.getIdCarrera(),
                c.getCodigoCarrera(),
                c.getNombreCarrera(),
                facultadNombre,
                null,
                null
        );
    }
}