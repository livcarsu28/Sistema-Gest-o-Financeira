package financeiro.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import financeiro.config.SecurityConfig;
import financeiro.repository.UsuarioRepository.DadosUsuario;
import financeiro.service.UsuarioService;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(usuarioService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void usuarioComumNaoPodeAcessarUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeListarUsuarios() throws Exception {
        when(usuarioService.listarUsuarios()).thenReturn(List.of(
                new DadosUsuario(1, "Admin", "admin@ong.org", "Administrador", "ADMIN", true)
        ));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@ong.org"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCadastraUsuarioComumComSucesso() throws Exception {
        when(usuarioService.cadastrarUsuarioComum("Comum", "comum@ong.org", "123456"))
                .thenReturn(new DadosUsuario(2, "Comum", "comum@ong.org", "Usuário Comum", "USER", true));

        mockMvc.perform(post("/usuarios").with(csrf())
                        .param("nome", "Comum")
                        .param("email", "comum@ong.org")
                        .param("senha", "123456"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("comum@ong.org"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void usuarioComumNaoPodeCadastrarUsuario() throws Exception {
        mockMvc.perform(post("/usuarios").with(csrf())
                        .param("nome", "Outro")
                        .param("email", "outro@ong.org")
                        .param("senha", "123456"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }
}
