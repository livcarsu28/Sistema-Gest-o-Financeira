package financeiro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import financeiro.config.SecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(FinanceiroController.class)
@Import(SecurityConfig.class)
class ConfiguracoesControllerTests {

    @Autowired
    private MockMvc mockMvc;

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
                .andExpect(content().string(containsString("Perfil de acesso")))
                .andExpect(content().string(containsString("Imagens")))
                .andExpect(content().string(containsString("Chave PIX")))
                .andExpect(content().string(containsString("Endereço da instituição")))
                .andExpect(content().string(containsString("Notificações")))
                .andExpect(content().string(containsString("/css/configuracoes.css")))
                .andExpect(content().string(containsString("/js/configuracoes.js")));
    }
}

