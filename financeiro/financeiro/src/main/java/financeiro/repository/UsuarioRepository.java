package financeiro.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import financeiro.security.UsuarioAutenticado;

@Repository
public class UsuarioRepository {

    private final JdbcTemplate jdbcTemplate;

    public UsuarioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UsuarioAutenticado> buscarPorEmail(String email) {
        String sql = """
                SELECT u.id_usuario, u.nome_completo, u.email, u.senha_hash,
                       u.status, u.data_exclusao, c.nivel_permissao
                FROM usuario u
                INNER JOIN cargo c ON u.id_cargo = c.id_cargo
                WHERE u.email = ?
                """;

        List<UsuarioAutenticado> resultados = jdbcTemplate.query(sql, new UsuarioRowMapper(), email);
        return resultados.stream().findFirst();
    }

    public int contarUsuariosAtivos() {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE data_exclusao IS NULL", Integer.class);
        return total != null ? total : 0;
    }

    public int contarUsuariosAtivosComBloqueio() {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE data_exclusao IS NULL FOR UPDATE", Integer.class);
        return total != null ? total : 0;
    }

    public int contarUsuariosPorNivel(String nivelPermissao) {
        String sql = """
                SELECT COUNT(*)
                FROM usuario u
                INNER JOIN cargo c ON u.id_cargo = c.id_cargo
                WHERE u.data_exclusao IS NULL
                  AND (UPPER(c.nivel_permissao) = UPPER(?) OR UPPER(c.nome) = UPPER(?))
                """;
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, nivelPermissao, nivelPermissao);
        return total != null ? total : 0;
    }

    public List<DadosUsuario> listarUsuarios() {
        String sql = """
                SELECT u.id_usuario, u.nome_completo, u.email, c.nome AS cargo,
                       c.nivel_permissao, u.status
                FROM usuario u
                INNER JOIN cargo c ON u.id_cargo = c.id_cargo
                WHERE u.data_exclusao IS NULL
                ORDER BY u.id_usuario
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DadosUsuario(
                rs.getInt("id_usuario"),
                rs.getString("nome_completo"),
                rs.getString("email"),
                rs.getString("cargo"),
                rs.getString("nivel_permissao"),
                rs.getBoolean("status")));
    }

    public Optional<Integer> buscarIdCargoPorNivel(String nivelPermissao) {
        String sql = "SELECT id_cargo FROM cargo WHERE UPPER(nivel_permissao) = UPPER(?) OR UPPER(nome) = UPPER(?) LIMIT 1";
        List<Integer> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("id_cargo"), nivelPermissao, nivelPermissao);
        if (!ids.isEmpty()) {
            return Optional.of(ids.get(0));
        }
        return Optional.empty();
    }

    public int criarCargoSeNaoExistir(String nome, String descricao, String nivelPermissao) {
        Optional<Integer> existente = buscarIdCargoPorNivel(nivelPermissao);
        if (existente.isPresent()) {
            return existente.get();
        }
        jdbcTemplate.update("INSERT INTO cargo (nome, descricao, nivel_permissao) VALUES (?, ?, ?)",
                nome, descricao, nivelPermissao);
        return buscarIdCargoPorNivel(nivelPermissao).orElse(1);
    }

    public int salvarUsuario(String nomeCompleto, String email, String senhaHash, int idCargo) {
        return jdbcTemplate.update(
                "INSERT INTO usuario (nome_completo, email, senha_hash, status, id_cargo) VALUES (?, ?, ?, TRUE, ?)",
                nomeCompleto, email, senhaHash, idCargo);
    }

    public record DadosUsuario(int id, String nomeCompleto, String email, String cargo, String nivelPermissao, boolean status) {}

    private static class UsuarioRowMapper implements RowMapper<UsuarioAutenticado> {
        @Override
        public UsuarioAutenticado mapRow(ResultSet rs, int rowNum) throws SQLException {
            Integer id = rs.getInt("id_usuario");
            String nome = rs.getString("nome_completo");
            String email = rs.getString("email");
            String senhaHash = rs.getString("senha_hash");
            boolean status = rs.getBoolean("status");
            Timestamp dataExclusaoTs = rs.getTimestamp("data_exclusao");
            LocalDateTime dataExclusao = dataExclusaoTs != null ? dataExclusaoTs.toLocalDateTime() : null;
            String nivelPermissao = rs.getString("nivel_permissao");

            return new UsuarioAutenticado(id, nome, email, senhaHash, status, dataExclusao, nivelPermissao);
        }
    }
}
