package financeiro.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import financeiro.dto.ItemMesRelatorioDTO;
import financeiro.dto.ItemMovimentacaoRelatorioDTO;
import financeiro.dto.PublicacaoFinanceiraDTO;
import financeiro.dto.TotalizadorCategoriaDTO;

@Repository
public class RelatorioRepository {

    private final JdbcTemplate jdbcTemplate;

    public RelatorioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResumoPeriodoRaw consultarTotaisPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COALESCE(SUM(CASE
                        WHEN tipo = 'ENTRADA' AND status_pagamento = 'PAGO'
                             AND COALESCE(data_pagamento, data_movimentacao) >= ?
                             AND COALESCE(data_pagamento, data_movimentacao) < ?
                        THEN valor ELSE 0 END), 0) AS entradas,
                    COALESCE(SUM(CASE
                        WHEN tipo = 'SAIDA' AND status_pagamento = 'PAGO'
                             AND COALESCE(data_pagamento, data_movimentacao) >= ?
                             AND COALESCE(data_pagamento, data_movimentacao) < ?
                        THEN valor ELSE 0 END), 0) AS saidas,
                    COALESCE(SUM(CASE
                        WHEN status_pagamento IN ('PENDENTE', 'VENCIDO')
                             AND data_movimentacao >= ? AND data_movimentacao < ?
                        THEN valor ELSE 0 END), 0) AS pendentes,
                    COUNT(CASE WHEN data_movimentacao >= ? AND data_movimentacao < ?
                               THEN 1 END) AS quantidade
                FROM movimentacao
                WHERE (data_movimentacao >= ? AND data_movimentacao < ?)
                   OR (status_pagamento = 'PAGO'
                       AND COALESCE(data_pagamento, data_movimentacao) >= ?
                       AND COALESCE(data_pagamento, data_movimentacao) < ?)
                """,
                (rs, rowNum) -> new ResumoPeriodoRaw(
                        rs.getBigDecimal("entradas"),
                        rs.getBigDecimal("saidas"),
                        rs.getBigDecimal("pendentes"),
                        rs.getLong("quantidade")),
                inicio, fim, inicio, fim, inicio, fim, inicio, fim, inicio, fim, inicio, fim);
    }

    public List<ItemMovimentacaoRelatorioDTO> listarMovimentacoesPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return jdbcTemplate.query("""
                SELECT
                    m.id_movimentacao,
                    m.data_movimentacao,
                    m.tipo,
                    m.valor,
                    m.descricao,
                    c.nome AS categoria,
                    m.status_pagamento,
                    COALESCE(fp.nome, 'Não informada') AS forma_pagamento
                FROM movimentacao m
                INNER JOIN categoria_movimentacao c ON m.id_categoria = c.id_categoria
                LEFT JOIN forma_pagamento fp ON m.id_forma_pagamento = fp.id_forma_pagamento
                WHERE (m.data_movimentacao >= ? AND m.data_movimentacao < ?)
                   OR (m.status_pagamento = 'PAGO'
                       AND COALESCE(m.data_pagamento, m.data_movimentacao) >= ?
                       AND COALESCE(m.data_pagamento, m.data_movimentacao) < ?)
                ORDER BY m.data_movimentacao DESC, m.id_movimentacao DESC
                """,
                (rs, rowNum) -> new ItemMovimentacaoRelatorioDTO(
                        rs.getInt("id_movimentacao"),
                        rs.getTimestamp("data_movimentacao").toLocalDateTime(),
                        rs.getString("tipo"),
                        rs.getBigDecimal("valor"),
                        rs.getString("descricao"),
                        rs.getString("categoria"),
                        rs.getString("status_pagamento"),
                        rs.getString("forma_pagamento")),
                inicio, fim, inicio, fim);
    }

    public List<ItemMesConsolidadoRaw> consultarMesesAno(LocalDateTime inicioAno, LocalDateTime fimAno) {
        return jdbcTemplate.query("""
                SELECT
                    MONTH(COALESCE(data_pagamento, data_movimentacao)) AS mes,
                    COALESCE(SUM(CASE
                        WHEN tipo = 'ENTRADA' AND status_pagamento = 'PAGO'
                        THEN valor ELSE 0 END), 0) AS total_entradas,
                    COALESCE(SUM(CASE
                        WHEN tipo = 'SAIDA' AND status_pagamento = 'PAGO'
                        THEN valor ELSE 0 END), 0) AS total_saidas,
                    COUNT(id_movimentacao) AS quantidade
                FROM movimentacao
                WHERE COALESCE(data_pagamento, data_movimentacao) >= ?
                  AND COALESCE(data_pagamento, data_movimentacao) < ?
                GROUP BY MONTH(COALESCE(data_pagamento, data_movimentacao))
                ORDER BY mes ASC
                """,
                (rs, rowNum) -> new ItemMesConsolidadoRaw(
                        rs.getInt("mes"),
                        rs.getBigDecimal("total_entradas"),
                        rs.getBigDecimal("total_saidas"),
                        rs.getLong("quantidade")),
                inicioAno, fimAno);
    }

    public List<TotalizadorCategoriaRaw> consultarTotaisPorCategoria(LocalDateTime inicio, LocalDateTime fim) {
        return jdbcTemplate.query("""
                SELECT
                    c.id_categoria,
                    c.nome AS categoria,
                    m.tipo,
                    COALESCE(SUM(m.valor), 0) AS total_valor,
                    COUNT(m.id_movimentacao) AS quantidade
                FROM movimentacao m
                INNER JOIN categoria_movimentacao c ON m.id_categoria = c.id_categoria
                WHERE ((m.data_movimentacao >= ? AND m.data_movimentacao < ?)
                   OR (m.status_pagamento = 'PAGO'
                       AND COALESCE(m.data_pagamento, m.data_movimentacao) >= ?
                       AND COALESCE(m.data_pagamento, m.data_movimentacao) < ?))
                GROUP BY c.id_categoria, c.nome, m.tipo
                ORDER BY m.tipo ASC, total_valor DESC
                """,
                (rs, rowNum) -> new TotalizadorCategoriaRaw(
                        rs.getInt("id_categoria"),
                        rs.getString("categoria"),
                        rs.getString("tipo"),
                        rs.getBigDecimal("total_valor"),
                        rs.getLong("quantidade")),
                inicio, fim, inicio, fim);
    }

    public int salvarPublicacao(int mes, int ano, BigDecimal totalGastos, BigDecimal totalEntradas,
                                String status, String observacao, Integer idUsuario) {
        return jdbcTemplate.update("""
                INSERT INTO publicacao_financeira
                    (mes_referencia, ano_referencia, total_gastos, total_entradas, status_atualizacao, observacao, id_usuario)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                mes, ano, totalGastos, totalEntradas, status, observacao, idUsuario);
    }

    public Optional<PublicacaoFinanceiraDTO> consultarUltimaPublicacao() {
        var lista = jdbcTemplate.query("""
                SELECT
                    id_publicacao,
                    mes_referencia,
                    ano_referencia,
                    total_gastos,
                    total_entradas,
                    data_publicacao,
                    status_atualizacao,
                    observacao
                FROM publicacao_financeira
                ORDER BY data_publicacao DESC, id_publicacao DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new PublicacaoFinanceiraDTO(
                        rs.getInt("id_publicacao"),
                        rs.getInt("mes_referencia"),
                        rs.getInt("ano_referencia"),
                        null,
                        rs.getBigDecimal("total_gastos"),
                        rs.getBigDecimal("total_entradas"),
                        rs.getTimestamp("data_publicacao").toLocalDateTime(),
                        rs.getString("status_atualizacao"),
                        rs.getString("observacao"),
                        true));
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    public record ResumoPeriodoRaw(BigDecimal entradas, BigDecimal saidas, BigDecimal pendentes, long quantidade) {}
    public record ItemMesConsolidadoRaw(int mes, BigDecimal entradas, BigDecimal saidas, long quantidade) {}
    public record TotalizadorCategoriaRaw(int idCategoria, String categoria, String tipo, BigDecimal total, long quantidade) {}
}
