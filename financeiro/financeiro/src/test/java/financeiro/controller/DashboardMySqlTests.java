package financeiro.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "DASHBOARD_MYSQL_TEST", matches = "true")
@SpringBootTest(properties = {"spring.flyway.enabled=false", "spring.datasource.hikari.read-only=true"})
@AutoConfigureMockMvc
@Transactional(readOnly = true)
class DashboardMySqlTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void dashboardCorrespondeAosRegistrosExistentesNoMySql() throws Exception {
        var periodo = YearMonth.now(ZoneId.of("America/Sao_Paulo"));
        var inicio = periodo.atDay(1).atStartOfDay();
        var fim = periodo.plusMonths(1).atDay(1).atStartOfDay();
        var entradas = consultarTotalPago("ENTRADA", inicio, fim);
        var saidas = consultarTotalPago("SAIDA", inicio, fim);
        var quantidade = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM movimentacao
                WHERE data_movimentacao >= ? AND data_movimentacao < ?
                """, Long.class, inicio, fim);
        var moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("entradasFormatadas", moeda.format(entradas)))
                .andExpect(model().attribute("saidasFormatadas", moeda.format(saidas)))
                .andExpect(model().attribute("saldoFormatado", moeda.format(entradas.subtract(saidas))))
                .andExpect(model().attribute("quantidadeMovimentacoes", quantidade))
                .andExpect(content().string(containsString(moeda.format(entradas))))
                .andExpect(content().string(containsString(moeda.format(saidas))))
                .andExpect(content().string(containsString(moeda.format(entradas.subtract(saidas)))))
                .andExpect(content().string(containsString(">" + quantidade + "</dd>")));
    }

    private BigDecimal consultarTotalPago(String tipo, LocalDateTime inicio, LocalDateTime fim) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(valor), 0) FROM movimentacao
                WHERE tipo = ? AND status_pagamento = 'PAGO'
                  AND data_pagamento >= ? AND data_pagamento < ?
                """, BigDecimal.class, tipo, inicio, fim);
    }
}
