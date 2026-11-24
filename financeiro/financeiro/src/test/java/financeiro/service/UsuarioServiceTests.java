package financeiro.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import financeiro.repository.UsuarioRepository;
import financeiro.repository.UsuarioRepository.DadosUsuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTests {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, passwordEncoder);
    }

    @Test
    void permiteCadastrarQuandoHaMenosDeDoisUsuarios() {
        when(repository.contarUsuariosAtivos()).thenReturn(1);
        assertTrue(service.podeCadastrarUsuarioComum());
    }

    @Test
    void impedeCadastroQuandoAtingeDoisUsuarios() {
        when(repository.contarUsuariosAtivos()).thenReturn(2);
        assertFalse(service.podeCadastrarUsuarioComum());
    }

    @Test
    void cadastraUsuarioComumComSucesso() {
        when(repository.contarUsuariosAtivosComBloqueio()).thenReturn(1);
        when(repository.contarUsuariosPorNivel("USER")).thenReturn(0);
        when(repository.buscarPorEmail("comum@ong.org")).thenReturn(Optional.empty());
        when(repository.buscarIdCargoPorNivel("USER")).thenReturn(Optional.of(2));
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$hashedPassword");
        when(repository.listarUsuarios()).thenReturn(List.of(
                new DadosUsuario(1, "Admin", "admin@ong.org", "Administrador", "ADMIN", true),
                new DadosUsuario(2, "Comum", "comum@ong.org", "Usuário Comum", "USER", true)
        ));

        DadosUsuario resultado = service.cadastrarUsuarioComum("Comum", "comum@ong.org", "senha123");

        assertEquals("comum@ong.org", resultado.email());
        assertEquals("USER", resultado.nivelPermissao());
        verify(repository).salvarUsuario("Comum", "comum@ong.org", "$2a$10$hashedPassword", 2);
    }

    @Test
    void lancaExcecaoAoUltrapassarLimiteDeDoisUsuarios() {
        when(repository.contarUsuariosAtivosComBloqueio()).thenReturn(2);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.cadastrarUsuarioComum("Terceiro", "terceiro@ong.org", "123456"));

        assertTrue(ex.getMessage().contains("limite de 2 usuários"));
    }

    @Test
    void lancaExcecaoQuandoJaExisteUmUsuarioComum() {
        when(repository.contarUsuariosAtivosComBloqueio()).thenReturn(1);
        when(repository.contarUsuariosPorNivel("USER")).thenReturn(1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.cadastrarUsuarioComum("OutroComum", "outro@ong.org", "123456"));

        assertTrue(ex.getMessage().contains("Já existe um Usuário Comum"));
    }

    @Test
    void lancaExcecaoComEmailDuplicado() {
        when(repository.contarUsuariosAtivosComBloqueio()).thenReturn(1);
        when(repository.contarUsuariosPorNivel("USER")).thenReturn(0);
        when(repository.buscarPorEmail("duplicado@ong.org"))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(financeiro.security.UsuarioAutenticado.class)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.cadastrarUsuarioComum("Nome", "duplicado@ong.org", "123456"));

        assertTrue(ex.getMessage().contains("Já existe um usuário"));
    }

    @Test
    void validaCamposObrigatorios() {
        assertThrows(IllegalArgumentException.class, () ->
                service.cadastrarUsuarioComum("", "email@ong.org", "123456"));

        assertThrows(IllegalArgumentException.class, () ->
                service.cadastrarUsuarioComum("Nome", "invalido", "123456"));

        assertThrows(IllegalArgumentException.class, () ->
                service.cadastrarUsuarioComum("Nome", "email@ong.org", "123"));
    }
}
