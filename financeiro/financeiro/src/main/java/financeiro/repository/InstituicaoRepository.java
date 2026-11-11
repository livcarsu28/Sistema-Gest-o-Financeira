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
                "SELECT id_instituicao, chave_pix, logradouro, numero, bairro, cidade, uf, cep FROM instituicao ORDER BY id_instituicao LIMIT 2"
                        + (bloquear ? " FOR UPDATE" : ""),
                (rs, rowNum) -> new Instituicao(
                        rs.getInt("id_instituicao"),
                        rs.getString("chave_pix"),
                        rs.getString("logradouro"),
                        rs.getString("numero"),
                        rs.getString("bairro"),
                        rs.getString("cidade"),
                        rs.getString("uf"),
                        rs.getString("cep")));
    }

    public int atualizarChavePix(int id, String chave) {
        return jdbcTemplate.update("UPDATE instituicao SET chave_pix = ? WHERE id_instituicao = ?", chave, id);
    }

    // Novo método para salvar o endereço mantendo o padrão da equipe
    public int atualizarEndereco(int id, String logradouro, String numero, String bairro, String cidade, String uf,
            String cep) {
        return jdbcTemplate.update(
                "UPDATE instituicao SET logradouro = ?, numero = ?, bairro = ?, cidade = ?, uf = ?, cep = ? WHERE id_instituicao = ?",
                logradouro, numero, bairro, cidade, uf, cep, id);
    }

    // Record atualizado com as colunas novas e construtor de compatibilidade
    public record Instituicao(int id, String chavePix, String logradouro, String numero, String bairro, String cidade,
            String uf, String cep) {

        // Este construtor extra impede que os testes antigos da equipe quebrem
        public Instituicao(int id, String chavePix) {
            this(id, chavePix, null, null, null, null, null, null);
        }
    }
}