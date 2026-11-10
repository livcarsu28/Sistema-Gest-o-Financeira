package financeiro.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InstituicaoRepository {

    private final JdbcTemplate jdbcTemplate;

    public InstituicaoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Instituicao> consultar(boolean bloquear) {
        return jdbcTemplate.query(
                "SELECT id_instituicao, chave_pix FROM instituicao ORDER BY id_instituicao LIMIT 2"
                        + (bloquear ? " FOR UPDATE" : ""),
                (rs, rowNum) -> new Instituicao(rs.getInt("id_instituicao"), rs.getString("chave_pix")));
    }

    public int atualizarChavePix(int id, String chave) {
        return jdbcTemplate.update("UPDATE instituicao SET chave_pix = ? WHERE id_instituicao = ?", chave, id);
    }

    public record Instituicao(int id, String chavePix) {
    }
}
