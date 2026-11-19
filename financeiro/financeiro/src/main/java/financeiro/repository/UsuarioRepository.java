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
