package financeiro.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ImagemRepository {

    private final JdbcTemplate jdbcTemplate;

    public ImagemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int contarTotal() {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM imagem_midia", Integer.class);
        return total != null ? total : 0;
    }

    public List<DadosImagemMidia> listarTodas() {
        String sql = """
                SELECT id_midia, categoria, url_imagem, data_upload, id_usuario
                FROM imagem_midia
                ORDER BY id_midia DESC
                """;
        return jdbcTemplate.query(sql, new ImagemRowMapper());
    }

    public Optional<DadosImagemMidia> buscarPorId(int idMidia) {
        String sql = """
                SELECT id_midia, categoria, url_imagem, data_upload, id_usuario
                FROM imagem_midia
                WHERE id_midia = ?
                """;
        List<DadosImagemMidia> lista = jdbcTemplate.query(sql, new ImagemRowMapper(), idMidia);
        return lista.stream().findFirst();
    }

    public int salvar(String categoria, String urlImagem, Integer idUsuario) {
        return jdbcTemplate.update(
                "INSERT INTO imagem_midia (categoria, url_imagem, id_usuario) VALUES (?, ?, ?)",
                categoria, urlImagem, idUsuario != null ? idUsuario : 1);
    }

    public int excluir(int idMidia) {
        return jdbcTemplate.update("DELETE FROM imagem_midia WHERE id_midia = ?", idMidia);
    }

    public record DadosImagemMidia(
            int id,
            String categoria,
            String urlImagem,
            LocalDateTime dataUpload,
            int idUsuario) {}

    private static class ImagemRowMapper implements RowMapper<DadosImagemMidia> {
        @Override
        public DadosImagemMidia mapRow(ResultSet rs, int rowNum) throws SQLException {
            var dataTs = rs.getTimestamp("data_upload");
            LocalDateTime data = dataTs != null ? dataTs.toLocalDateTime() : LocalDateTime.now();
            return new DadosImagemMidia(
                    rs.getInt("id_midia"),
                    rs.getString("categoria"),
                    rs.getString("url_imagem"),
                    data,
                    rs.getInt("id_usuario"));
        }
    }
}
