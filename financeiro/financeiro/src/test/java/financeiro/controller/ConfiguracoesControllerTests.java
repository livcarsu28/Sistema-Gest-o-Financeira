package financeiro.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import financeiro.config.SecurityConfig;
import financeiro.repository.InstituicaoRepository;
import financeiro.service.DashboardService;
import financeiro.service.PixService;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({FinanceiroController.class, ConfiguracoesController.class})
@Import(SecurityConfig.class)
class ConfiguracoesControllerTests {

    @MockitoBean
    private PixService pixService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void configurarPix() {
        when(pixService.consultar()).thenReturn(
                new PixService.DadosPix(null, "Nenhuma chave Pix cadastrada."));
    }

    @Test
    void configuracoesSemAutenticacaoRedirecionaParaLogin() throws Exception {
        mockMvc.perform(get("/configuracoes").accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void recursosEstaticosDeConfiguracoesDisponiveisSemAutenticacao() throws Exception {
        mockMvc.perform(get("/css/configuracoes.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/configuracoes.js")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void configuracoesAutenticadoRenderizaTemplateComSucesso() throws Exception {
        mockMvc.perform(get("/configuracoes").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracoes"))
                .andExpect(content().string(containsString("RESGATE DE VIDAS")))
                .andExpect(content().string(containsString("Ajustes")))
                .andExpect(content().string(containsString("Personalize as configurações")))
                .andExpect(content().string(containsString("Perfil de Acesso:")))
                .andExpect(content().string(containsString("Sair")))
                .andExpect(content().string(containsString("Imagens")))
                .andExpect(content().string(containsString("Chave PIX")))
                .andExpect(content().string(containsString("Endereço da instituição")))
                .andExpect(content().string(containsString("/css/configuracoes.css")))
                .andExpect(content().string(containsString("/js/configuracoes.js")));
    }

    @Test
    void atualizarEnderecoSemAutenticacaoExigeAutenticacao() throws Exception {
        mockMvc.perform(post("/configuracoes/endereco").with(csrf())
                        .param("logradouro", "Rua Nova")
                        .param("numero", "10")
                        .param("bairro", "Centro")
                        .param("cidade", "São Paulo")
                        .param("uf", "SP")
                        .param("cep", "01001-000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void atualizarEnderecoSemCsrfERecusado() throws Exception {
        mockMvc.perform(post("/configuracoes/endereco")
                        .param("logradouro", "Rua Nova")
                        .param("numero", "10")
                        .param("bairro", "Centro")
                        .param("cidade", "São Paulo")
                        .param("uf", "SP")
                        .param("cep", "01001-000"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void atualizarEnderecoPorUsuarioComumERecusado() throws Exception {
        mockMvc.perform(post("/configuracoes/endereco").with(csrf())
                        .param("logradouro", "Rua Nova")
                        .param("numero", "10")
                        .param("bairro", "Centro")
                        .param("cidade", "São Paulo")
                        .param("uf", "SP")
                        .param("cep", "01001-000"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void atualizarEnderecoComSucesso() throws Exception {
        mockMvc.perform(post("/configuracoes/endereco").with(csrf())
                        .param("logradouro", "Rua da Esperança")
                        .param("numero", "128")
                        .param("bairro", "Centro")
                        .param("cidade", "São Paulo")
                        .param("uf", "SP")
                        .param("cep", "01010-001"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/configuracoes"))
                .andExpect(flash().attribute("sucesso", "Endereço atualizado com sucesso!"));

        verify(instituicaoRepository).atualizarEndereco(
                1, "Rua da Esperança", "128", "Centro", "São Paulo", "SP", "01010-001");
    }
}
