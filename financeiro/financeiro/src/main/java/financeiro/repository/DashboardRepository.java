package financeiro.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResumoMensal consultarResumoMensal(LocalDateTime inicio, LocalDateTime fim) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(CASE
                           WHEN tipo = 'ENTRADA' AND status_pagamento = 'PAGO'
                                AND data_pagamento >= ? AND data_pagamento < ?
                           THEN valor ELSE 0 END), 0) AS entradas,
                       COALESCE(SUM(CASE
                           WHEN tipo = 'SAIDA' AND status_pagamento = 'PAGO'
                                AND data_pagamento >= ? AND data_pagamento < ?
                           THEN valor ELSE 0 END), 0) AS saidas,
                       COUNT(CASE WHEN data_movimentacao >= ? AND data_movimentacao < ?
                                  THEN 1 END) AS quantidade
                FROM movimentacao
                WHERE (data_movimentacao >= ? AND data_movimentacao < ?)
                   OR (status_pagamento = 'PAGO' AND data_pagamento >= ? AND data_pagamento < ?)
                """,
                (rs, rowNum) -> new ResumoMensal(
                        rs.getBigDecimal("entradas"),
                        rs.getBigDecimal("saidas"),
                        rs.getLong("quantidade")),
                inicio, fim, inicio, fim, inicio, fim, inicio, fim, inicio, fim);
    }

    public record ResumoMensal(BigDecimal entradas, BigDecimal saidas, long quantidadeMovimentacoes) {
    }
}
