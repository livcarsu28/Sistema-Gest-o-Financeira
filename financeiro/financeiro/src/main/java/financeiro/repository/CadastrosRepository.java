package financeiro.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositório de leitura para os cadastros-base utilizados nos formulários de movimentações:
 * categorias de movimentação e formas de pagamento.
 *
 * Ambas as tabelas possuem estrutura análoga: id, nome e ativo (exclusão lógica).
 * Apenas registros com ativo = TRUE são retornados, pois registros inativados
 * não devem aparecer como opção em novos lançamentos.
 */
@Repository
public class CadastrosRepository {

    private final JdbcTemplate jdbcTemplate;

    public CadastrosRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CategoriaMovimentacao> listarCategorias() {
        return jdbcTemplate.query(
                "SELECT id_categoria, nome FROM categoria_movimentacao WHERE ativo = TRUE ORDER BY nome",
                (rs, rowNum) -> new CategoriaMovimentacao(
                        rs.getInt("id_categoria"),
                        rs.getString("nome")));
    }

    public List<FormaPagamento> listarFormasPagamento() {
        return jdbcTemplate.query(
                "SELECT id_forma_pagamento, nome FROM forma_pagamento WHERE ativo = TRUE ORDER BY nome",
                (rs, rowNum) -> new FormaPagamento(
                        rs.getInt("id_forma_pagamento"),
                        rs.getString("nome")));
    }

    // -------------------------------------------------------------------------
    // Modelos de dados — records imutáveis, sem dependência de JPA
    // -------------------------------------------------------------------------

    /**
     * Categoria de movimentação ativa.
     * Não carrega a coluna "ativo" porque o filtro já é feito na query.
     */
    public record CategoriaMovimentacao(int id, String nome) {}

    /**
     * Forma de pagamento ativa.
     * id_forma_pagamento é nullable em movimentacao (a FK admite NULL),
     * portanto a forma de pagamento é opcional no lançamento.
     */
    public record FormaPagamento(int id, String nome) {}
}
