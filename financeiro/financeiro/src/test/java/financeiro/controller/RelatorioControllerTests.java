package financeiro.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import financeiro.config.SecurityConfig;
import financeiro.config.TestSecurityConfig;
import financeiro.dto.PublicacaoFinanceiraDTO;
import financeiro.dto.RelatorioAnualDTO;
import financeiro.dto.RelatorioMensalDTO;
import financeiro.service.RelatorioService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RelatorioController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class RelatorioControllerTests {

    @MockitoBean
    private RelatorioService relatorioService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setupMocks() {
        RelatorioMensalDTO mensal = new RelatorioMensalDTO(
                11, 2026, "Novembro de 2026",
                new BigDecimal("6000.00"), new BigDecimal("3500.00"), new BigDecimal("2500.00"),
                BigDecimal.ZERO, 5L, Collections.emptyList()
        );
        RelatorioAnualDTO anual = new RelatorioAnualDTO(
                2026, new BigDecimal("6000.00"), new BigDecimal("3500.00"), new BigDecimal("2500.00"),
                new BigDecimal("291.67"), Collections.emptyList()
        );
        PublicacaoFinanceiraDTO pub = new PublicacaoFinanceiraDTO(
                1, 11, 2026, "Novembro de 2026",
                new BigDecimal("3500.00"), new BigDecimal("6000.00"),
                LocalDateTime.now(), "Atualizado", "OK", true
        );

        when(relatorioService.gerarRelatorioMensal(anyInt(), anyInt())).thenReturn(mensal);
        when(relatorioService.gerarRelatorioAnual(anyInt())).thenReturn(anual);
        when(relatorioService.gerarTotalizadoresPorCategoria(anyInt(), anyInt())).thenReturn(Collections.emptyList());
        when(relatorioService.consultarUltimaPublicacao()).thenReturn(pub);
    }

    @Test
    void relatoriosSemAutenticacaoRedirecionaParaLogin() throws Exception {
        mockMvc.perform(get("/relatorios").accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser
    void relatoriosAutenticadoRenderizaTemplate() throws Exception {
        mockMvc.perform(get("/relatorios").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("relatorios"))
                .andExpect(content().string(containsString("Relatórios Financeiros")))
                .andExpect(content().string(containsString("Novembro de 2026")))
                .andExpect(content().string(containsString("R$\u00a06.000,00")))
                .andExpect(content().string(containsString("R$\u00a03.500,00")));
    }

    @Test
    void apiPublicacaoTransparenciaEstaDisponivelSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/publicacao-transparencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusAtualizacao").value("Atualizado"))
                .andExpect(jsonPath("$.totalGastos").value(3500.00));
    }

    @Test
    @WithMockUser
    void apiMensalRetornaDadosJson() throws Exception {
        mockMvc.perform(get("/relatorios/api/mensal").param("mes", "11").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value(11))
                .andExpect(jsonPath("$.periodoFormatado").value("Novembro de 2026"));
    }

    @Test
    @WithMockUser
    void publicarRelatorioComCsrfRetornaSucesso() throws Exception {
        PublicacaoFinanceiraDTO pub = new PublicacaoFinanceiraDTO(
                2, 11, 2026, "Novembro de 2026",
                new BigDecimal("3500.00"), new BigDecimal("6000.00"),
                LocalDateTime.now(), "Atualizado", "Publicação feita", true
        );
        when(relatorioService.publicarDadosNoSite(eq(11), eq(2026), any(), any())).thenReturn(pub);

        mockMvc.perform(post("/relatorios/api/publicar")
                        .with(csrf())
                        .param("mes", "11")
                        .param("ano", "2026")
                        .param("observacao", "Publicação feita"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusAtualizacao").value("Atualizado"));
    }
}
