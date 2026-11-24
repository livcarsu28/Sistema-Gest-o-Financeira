package financeiro.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import financeiro.repository.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InicializadorPrimeiroAdminTests {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void provisionaPrimeiroAdminQuandoBancoVazioEVariaveisDefinidas() {
        when(usuarioRepository.contarUsuariosAtivos()).thenReturn(0);
        when(usuarioRepository.buscarIdCargoPorNivel("ADMIN")).thenReturn(Optional.of(1));
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("$2a$10$hashBCrypt");

        InicializadorPrimeiroAdmin inicializador = new InicializadorPrimeiroAdmin(
                usuarioRepository,
                passwordEncoder,
                "Diretor Geral",
                "admin@resgatedevidas.org",
                "senhaSegura123");

        boolean resultado = inicializador.inicializarPrimeiroAdministradorSeNecessario();

        assertTrue(resultado);
        verify(passwordEncoder).encode("senhaSegura123");
        verify(usuarioRepository).salvarUsuario(
                eq("Diretor Geral"),
                eq("admin@resgatedevidas.org"),
                eq("$2a$10$hashBCrypt"),
                eq(1));
    }

    @Test
    void naoProvisionaQuandoJaExistemUsuariosCadastrados() {
        when(usuarioRepository.contarUsuariosAtivos()).thenReturn(1);

        InicializadorPrimeiroAdmin inicializador = new InicializadorPrimeiroAdmin(
                usuarioRepository,
                passwordEncoder,
                "Admin",
                "admin@ong.org",
                "senha123");

        boolean resultado = inicializador.inicializarPrimeiroAdministradorSeNecessario();

        assertFalse(resultado);
        verify(usuarioRepository, never()).salvarUsuario(anyString(), anyString(), anyString(), anyInt());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void naoCriaCredenciaisFicticiasSeVariaveisEstiveremAusentes() {
        when(usuarioRepository.contarUsuariosAtivos()).thenReturn(0);

        InicializadorPrimeiroAdmin inicializador = new InicializadorPrimeiroAdmin(
                usuarioRepository,
                passwordEncoder,
                "Admin",
                "",
                "");

        boolean resultado = inicializador.inicializarPrimeiroAdministradorSeNecessario();

        assertFalse(resultado);
        verify(usuarioRepository, never()).salvarUsuario(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void naoProvisionaComSenhaCurtaOuEmailInvalido() {
        when(usuarioRepository.contarUsuariosAtivos()).thenReturn(0);

        // Senha curta (< 6)
        InicializadorPrimeiroAdmin inicializador1 = new InicializadorPrimeiroAdmin(
                usuarioRepository,
                passwordEncoder,
                "Admin",
                "admin@ong.org",
                "123");
        assertFalse(inicializador1.inicializarPrimeiroAdministradorSeNecessario());

        // E-mail inválido
        InicializadorPrimeiroAdmin inicializador2 = new InicializadorPrimeiroAdmin(
                usuarioRepository,
                passwordEncoder,
                "Admin",
                "email_invalido",
                "senhaSegura123");
        assertFalse(inicializador2.inicializarPrimeiroAdministradorSeNecessario());

        verify(usuarioRepository, never()).salvarUsuario(anyString(), anyString(), anyString(), anyInt());
    }
}
