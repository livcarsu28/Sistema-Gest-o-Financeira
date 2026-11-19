package financeiro.security;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioAutenticadoTests {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void usuarioAtivoEValido() {
        String hash = encoder.encode("senhaSegura123");
        var usuario = new UsuarioAutenticado(
                1, "Maria Silva", "maria@example.org", hash, true, null, "ADMIN");

        assertEquals(1, usuario.getId());
        assertEquals(1, usuario.getIdUsuario());
        assertEquals("Maria Silva", usuario.getNome());
        assertEquals("Maria Silva", usuario.getNomeCompleto());
        assertEquals("maria@example.org", usuario.getUsername());
        assertEquals("maria@example.org", usuario.getEmail());
        assertEquals(hash, usuario.getPassword());
        assertTrue(usuario.isStatus());
        assertNull(usuario.getDataExclusao());
        assertEquals("ADMIN", usuario.getNivelPermissao());

        assertTrue(usuario.isEnabled());
        assertTrue(usuario.isAccountNonLocked());
        assertTrue(usuario.isAccountNonExpired());
        assertTrue(usuario.isCredentialsNonExpired());
    }

    @Test
    void usuarioInativoEstaDesabilitado() {
        var usuario = new UsuarioAutenticado(
                2, "João Inativo", "joao@example.org", "hash", false, null, "OPERADOR");

        assertFalse(usuario.isEnabled());
        assertTrue(usuario.isAccountNonLocked());
    }

    @Test
    void usuarioComExclusaoLogicaEstaDesabilitadoEBloqueado() {
        LocalDateTime dataExclusao = LocalDateTime.of(2026, 11, 1, 10, 0);
        var usuario = new UsuarioAutenticado(
                3, "Carlos Excluído", "carlos@example.org", "hash", true, dataExclusao, "OPERADOR");

        assertFalse(usuario.isEnabled());
        assertFalse(usuario.isAccountNonLocked());
        assertEquals(dataExclusao, usuario.getDataExclusao());
    }

    @Test
    void converteNivelPermissaoParaRolePadrao() {
        var usuarioAdmin = new UsuarioAutenticado(
                1, "Admin", "admin@example.org", "hash", true, null, "ADMIN");
        assertTrue(usuarioAdmin.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var usuarioOperador = new UsuarioAutenticado(
                2, "Operador", "op@example.org", "hash", true, null, "operador");
        assertTrue(usuarioOperador.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OPERADOR")));

        var usuarioSemPermissao = new UsuarioAutenticado(
                3, "Sem Cargo", "sem@example.org", "hash", true, null, null);
        assertTrue(usuarioSemPermissao.getAuthorities().isEmpty());
    }

    @Test
    void validaSenhaComBCrypt() {
        String senhaPlana = "minhaSenhaSecreta";
        String hashGerado = encoder.encode(senhaPlana);

        var usuario = new UsuarioAutenticado(
                1, "Ana", "ana@example.org", hashGerado, true, null, "ADMIN");

        assertTrue(encoder.matches(senhaPlana, usuario.getPassword()));
        assertFalse(encoder.matches("senhaIncorreta", usuario.getPassword()));
    }

    @Test
    void toStringNaoExibeSenhaHash() {
        var usuario = new UsuarioAutenticado(
                1, "Ana", "ana@example.org", "$2a$10$superSecretHash", true, null, "ADMIN");

        String representacao = usuario.toString();
        assertFalse(representacao.contains("$2a$10$superSecretHash"));
        assertTrue(representacao.contains("ana@example.org"));
    }
}
