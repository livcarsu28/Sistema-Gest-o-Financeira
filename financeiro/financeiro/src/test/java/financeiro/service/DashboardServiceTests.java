package financeiro.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import financeiro.repository.DashboardRepository;
import financeiro.repository.DashboardRepository.ResumoMensal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceTests {

    private final DashboardRepository repository = mock(DashboardRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-12-01T01:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));
    private final LocalDateTime inicio = LocalDateTime.of(2026, 11, 1, 0, 0);
    private final LocalDateTime fim = LocalDateTime.of(2026, 12, 1, 0, 0);

    @Test
    void usaMesBrasileiroECalculaSaldoNegativoComPrecisao() {
        when(repository.consultarResumoMensal(inicio, fim)).thenReturn(
                new ResumoMensal(new BigDecimal("100.25"), new BigDecimal("200.10"), 5));

        var dados = new DashboardService(repository, clock).consultarDashboard();

        assertEquals(YearMonth.of(2026, 11), dados.periodo());
        assertEquals(new BigDecimal("100.25"), dados.entradas());
        assertEquals(new BigDecimal("200.10"), dados.saidas());
        assertEquals(new BigDecimal("-99.85"), dados.saldo());
        assertEquals(5, dados.quantidadeMovimentacoes());
        verify(repository).consultarResumoMensal(inicio, fim);
    }

    @Test
    void mantemZeroQuandoNaoHaMovimentacoes() {
        when(repository.consultarResumoMensal(inicio, fim)).thenReturn(
                new ResumoMensal(BigDecimal.ZERO, BigDecimal.ZERO, 0));

        var dados = new DashboardService(repository, clock).consultarDashboard();

        assertEquals(BigDecimal.ZERO, dados.entradas());
        assertEquals(BigDecimal.ZERO, dados.saidas());
        assertEquals(BigDecimal.ZERO, dados.saldo());
        assertEquals(0, dados.quantidadeMovimentacoes());
    }
}
