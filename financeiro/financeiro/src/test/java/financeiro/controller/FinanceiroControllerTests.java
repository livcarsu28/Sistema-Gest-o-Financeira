package financeiro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.context.annotation.Import;
import financeiro.config.SecurityConfig;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

@WebMvcTest(FinanceiroController.class)
@Import(SecurityConfig.class)
class FinanceiroControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityProperties securityProperties;

    @Test
    @WithMockUser
    void inicioRedirecionaParaDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void loginBemSucedidoRedirecionaParaDashboard() throws Exception {
        var resultado = mockMvc.perform(formLogin()
                        .user(securityProperties.getUser().getName())
                        .password(securityProperties.getUser().getPassword()))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();
        var sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        mockMvc.perform(get("/dashboard").session(sessao))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void arquivosVisuaisEstaoDisponiveisSemAutenticacao() throws Exception {
        for (var caminho : new String[]{"/css/dashboard.css", "/js/dashboard.js",
                "/fonts/poppins-400.ttf", "/fonts/poppins-500.ttf",
                "/fonts/poppins-600.ttf", "/fonts/poppins-700.ttf",
                "/img/logo-resgate-de-vidas.png"}) {
            mockMvc.perform(get(caminho)).andExpect(status().isOk());
        }
    }

    @Test
    void inicioSemAutenticacaoRedirecionaParaLogin() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginSemCsrfERecusado() throws Exception {
        mockMvc.perform(post("/login"))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardSemAutenticacaoRedirecionaParaLogin() throws Exception {
        mockMvc.perform(get("/dashboard").accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginPadraoEstaDisponivel() throws Exception {
        mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser
    void dashboardAutenticadoRenderizaTemplateAtual() throws Exception {
        mockMvc.perform(get("/dashboard").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("Olá, equipe.")))
                .andExpect(content().string(containsString("/img/logo-resgate-de-vidas.png")))
                .andExpect(content().string(containsString("Aguardando dados financeiros")));
    }
}
