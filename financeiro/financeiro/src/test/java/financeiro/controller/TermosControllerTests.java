package financeiro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import financeiro.config.SecurityConfig;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TermosController.class)
@Import(SecurityConfig.class)
class TermosControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void termosAcessivelPublicamenteSemAutenticacao() throws Exception {
        mockMvc.perform(get("/termos"))
                .andExpect(status().isOk())
                .andExpect(view().name("termos-e-politicas"))
                .andExpect(content().string(containsString("Termos de Uso do Sistema")))
                .andExpect(content().string(containsString("Resgate de Vidas")));
    }

    @Test
    void privacidadeAcessivelPublicamenteSemAutenticacao() throws Exception {
        mockMvc.perform(get("/privacidade"))
                .andExpect(status().isOk())
                .andExpect(view().name("termos-e-politicas"))
                .andExpect(content().string(containsString("Política de Privacidade")))
                .andExpect(content().string(containsString("LGPD")));
    }

    @Test
    void termosEPoliticasAcessivelSemAutenticacao() throws Exception {
        mockMvc.perform(get("/termos-e-politicas"))
                .andExpect(status().isOk())
                .andExpect(view().name("termos-e-politicas"));
    }
}
