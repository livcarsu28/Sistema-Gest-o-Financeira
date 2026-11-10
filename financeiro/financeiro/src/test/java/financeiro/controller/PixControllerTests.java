package financeiro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import financeiro.config.SecurityConfig;
import financeiro.service.PixService;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PixController.class)
@Import(SecurityConfig.class)
class PixControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PixService service;

    @Test
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(post("/configuracoes/pix").with(csrf()).param("chavePix", "contato@example.org"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser
    void exigeCsrf() throws Exception {
        mockMvc.perform(post("/configuracoes/pix").param("chavePix", "contato@example.org"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser
    void salvaComAutenticacaoECsrf() throws Exception {
        when(service.salvar("contato@example.org")).thenReturn(
                new PixService.DadosPix("contato@example.org", "Chave Pix atualizada com sucesso!"));
        mockMvc.perform(post("/configuracoes/pix").with(csrf()).param("chavePix", "contato@example.org"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chave").value("contato@example.org"));
    }

    @Test
    @WithMockUser
    void retornaErroDeValidacao() throws Exception {
        when(service.salvar("invalida")).thenThrow(new IllegalArgumentException("Informe uma chave Pix válida."));
        mockMvc.perform(post("/configuracoes/pix").with(csrf()).param("chavePix", "invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe uma chave Pix válida."));
    }

    @Test
    @WithMockUser
    void retornaConflitoSemInstituicao() throws Exception {
        when(service.salvar("contato@example.org")).thenThrow(new IllegalStateException("Cadastre a instituição."));
        mockMvc.perform(post("/configuracoes/pix").with(csrf()).param("chavePix", "contato@example.org"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void falhaDeBancoNaoExibeDetalhesInternos() throws Exception {
        when(service.salvar("contato@example.org"))
                .thenThrow(new DataAccessResourceFailureException("detalhe interno"));
        mockMvc.perform(post("/configuracoes/pix").with(csrf()).param("chavePix", "contato@example.org"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value("Não foi possível salvar a chave Pix. Tente novamente."));
    }
}
