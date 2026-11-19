package financeiro.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import financeiro.repository.UsuarioRepository;
import financeiro.security.UsuarioAutenticado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTests {

    @Mock
    private UsuarioRepository repository;

    private AutenticacaoService service;

    @BeforeEach
    void setUp() {
        service = new AutenticacaoService(repository);
    }

    @Test
    void carregaUsuarioExistentePorEmail() {
        var usuario = new UsuarioAutenticado(
                1, "Fulano", "fulano@example.org", "hash123", true, null, "ADMIN");
        when(repository.buscarPorEmail("fulano@example.org")).thenReturn(Optional.of(usuario));

        UserDetails resultado = service.loadUserByUsername("fulano@example.org");

        assertNotNull(resultado);
        assertEquals("fulano@example.org", resultado.getUsername());
        verify(repository).buscarPorEmail("fulano@example.org");
    }

    @Test
    void lancaExcecaoQuandoUsuarioNaoEncontrado() {
        when(repository.buscarPorEmail("desconhecido@example.org")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                service.loadUserByUsername("desconhecido@example.org"));

        verify(repository).buscarPorEmail("desconhecido@example.org");
    }

    @Test
    void lancaExcecaoQuandoEmailNuloOuEmBranco() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(null));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("   "));
        verifyNoInteractions(repository);
    }

    @Test
    void aparaEspacosEmBrancoNoEmail() {
        var usuario = new UsuarioAutenticado(
                1, "Fulano", "fulano@example.org", "hash123", true, null, "ADMIN");
        when(repository.buscarPorEmail("fulano@example.org")).thenReturn(Optional.of(usuario));

        UserDetails resultado = service.loadUserByUsername("  fulano@example.org  ");

        assertNotNull(resultado);
        verify(repository).buscarPorEmail("fulano@example.org");
    }
}
