package financeiro.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import financeiro.repository.DashboardRepository;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final Clock clock;

    @Autowired
    public DashboardService(DashboardRepository dashboardRepository) {
        this(dashboardRepository, Clock.system(ZoneId.of("America/Sao_Paulo")));
    }

    DashboardService(DashboardRepository dashboardRepository, Clock clock) {
        this.dashboardRepository = dashboardRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DadosDashboard consultarDashboard() {
        YearMonth periodo = YearMonth.now(clock);
        var resumo = dashboardRepository.consultarResumoMensal(
                periodo.atDay(1).atStartOfDay(),
                periodo.plusMonths(1).atDay(1).atStartOfDay());

        return new DadosDashboard(periodo, resumo.entradas(), resumo.saidas(),
                resumo.entradas().subtract(resumo.saidas()), resumo.quantidadeMovimentacoes());
    }

    public record DadosDashboard(YearMonth periodo, BigDecimal entradas, BigDecimal saidas,
            BigDecimal saldo, long quantidadeMovimentacoes) {
    }
}
