package financeiro.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import financeiro.security.UsuarioAutenticado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private UsuarioRepository repository;

    @BeforeEach
    void setUp() {
        repository = new UsuarioRepository(jdbcTemplate);
    }

    @Test
    void buscaUsuarioExistentePorEmail() {
        var usuarioEsperado = new UsuarioAutenticado(
                1, "Admin Sistema", "admin@example.org", "hash123", true, null, "ADMIN");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("admin@example.org")))
                .thenReturn(List.of(usuarioEsperado));

        Optional<UsuarioAutenticado> resultado = repository.buscarPorEmail("admin@example.org");

        assertTrue(resultado.isPresent());
        assertEquals(1, resultado.get().getId());
        assertEquals("admin@example.org", resultado.get().getEmail());
        assertEquals("ADMIN", resultado.get().getNivelPermissao());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq("admin@example.org"));
    }

    @Test
    void retornaVazioQuandoUsuarioNaoEncontrado() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("naoexiste@example.org")))
                .thenReturn(Collections.emptyList());

        Optional<UsuarioAutenticado> resultado = repository.buscarPorEmail("naoexiste@example.org");

        assertTrue(resultado.isEmpty());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq("naoexiste@example.org"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapeiaLinhaDoBancoCorretamente() throws SQLException {
        LocalDateTime dataExclusaoEsperada = LocalDateTime.of(2026, 11, 10, 15, 30);
        when(resultSet.getInt("id_usuario")).thenReturn(42);
        when(resultSet.getString("nome_completo")).thenReturn("Maria Teste");
        when(resultSet.getString("email")).thenReturn("maria@example.org");
        when(resultSet.getString("senha_hash")).thenReturn("hashSeguro");
        when(resultSet.getBoolean("status")).thenReturn(true);
        when(resultSet.getTimestamp("data_exclusao")).thenReturn(Timestamp.valueOf(dataExclusaoEsperada));
        when(resultSet.getString("nivel_permissao")).thenReturn("FINANCEIRO");

        // Simula a invocação do RowMapper capturando o argumento passado ao jdbcTemplate.query
        doAnswer(invocation -> {
            RowMapper<UsuarioAutenticado> mapper = invocation.getArgument(1);
            UsuarioAutenticado usuarioMapeado = mapper.mapRow(resultSet, 1);
            return List.of(usuarioMapeado);
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), eq("maria@example.org"));

        Optional<UsuarioAutenticado> resultado = repository.buscarPorEmail("maria@example.org");

        assertTrue(resultado.isPresent());
        UsuarioAutenticado usuario = resultado.get();
        assertEquals(42, usuario.getId());
        assertEquals("Maria Teste", usuario.getNome());
        assertEquals("maria@example.org", usuario.getEmail());
        assertEquals("hashSeguro", usuario.getPassword());
        assertTrue(usuario.isStatus());
        assertEquals(dataExclusaoEsperada, usuario.getDataExclusao());
        assertEquals("FINANCEIRO", usuario.getNivelPermissao());
    }
}
