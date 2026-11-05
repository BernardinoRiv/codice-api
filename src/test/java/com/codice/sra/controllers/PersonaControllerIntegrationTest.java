package com.codice.sra.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PersonaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Debe responder 404 al consultar un documento que no existe")
    void buscarPorDocumento_Inexistente_DebeRetornar404() throws Exception {
        mockMvc.perform(get("/api/v1/personas/buscar-por-documento")
                        .param("numeroDocumento", "00000000-0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DOCENTE") // Rol sin permisos para consultar datos civiles de terceros
    @DisplayName("Debe responder 403 Forbidden cuando el usuario no es ADMINISTRADOR")
    void buscarPorDocumento_RolNoAutorizado_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/v1/personas/buscar-por-documento")
                        .param("numeroDocumento", "05123456-7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}