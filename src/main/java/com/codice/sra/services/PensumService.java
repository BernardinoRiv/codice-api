package com.codice.sra.services;

import com.codice.sra.dtos.PensumDetalleDTO;
import com.codice.sra.dtos.PensumListadoDTO;
import com.codice.sra.dtos.PensumMateriaDetalleDTO;
import com.codice.sra.models.Materia;
import com.codice.sra.models.Pensum;
import com.codice.sra.models.PensumMateria;
import com.codice.sra.repositories.PensumMateriaRepository;
import com.codice.sra.repositories.PensumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PensumService {

    private final PensumRepository pensumRepository;
    private final PensumMateriaRepository pensumMateriaRepository;

    @Transactional(readOnly = true)
    public List<PensumListadoDTO> listarTodos() {
        return pensumRepository.findTodosParaAdministracion().stream().map(this::mapearCabecera).toList();
    }

    @Transactional(readOnly = true)
    public PensumDetalleDTO obtenerDetalle(Long idPensum) {
        if (idPensum == null || idPensum <= 0) throw new IllegalArgumentException("El ID del pensum debe ser positivo");
        Pensum pensum = pensumRepository.findDetalleCabecera(idPensum)
                .orElseThrow(() -> new IllegalArgumentException("No existe un pensum con ID: " + idPensum));
        List<PensumMateria> asignaciones = pensumMateriaRepository.findByPensumIdOrderByCicloRecomendado(idPensum);
        List<PensumMateriaDetalleDTO> materias = asignaciones.stream().map(pm -> {
            Materia materia = pm.getMateria();
            List<PensumMateriaDetalleDTO.PrerrequisitoDTO> prerrequisitos = materia.getPrerrequisitos() == null ? List.of() :
                    materia.getPrerrequisitos().stream().map(pr -> new PensumMateriaDetalleDTO.PrerrequisitoDTO(pr.getIdMateria(), pr.getCodigoMateria(), pr.getNombreMateria())).toList();
            return new PensumMateriaDetalleDTO(materia.getIdMateria(), materia.getCodigoMateria(), materia.getNombreMateria(),
                    materia.getUnidadesValorativas(), pm.getCicloRecomendado(), pm.getObligatoria(), prerrequisitos);
        }).toList();
        BigDecimal totalUv = materias.stream().map(PensumMateriaDetalleDTO::unidadesValorativas).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalCiclos = materias.stream().map(PensumMateriaDetalleDTO::cicloRecomendado).filter(c -> c != null).max(Integer::compareTo).orElse(0);
        return new PensumDetalleDTO(mapearCabecera(pensum), materias.size(), totalUv, totalCiclos, materias);
    }

    private PensumListadoDTO mapearCabecera(Pensum p) {
        return new PensumListadoDTO(p.getIdPensum(), p.getCarrera().getIdCarrera(), p.getCarrera().getCodigoCarrera(),
                p.getCarrera().getNombreCarrera(), p.getVersion(), p.getAnioInicio(), p.getAnioFin(),
                p.getEstadoPensum().getIdEstadoPensum(), p.getEstadoPensum().getEstadoPensum());
    }
}
