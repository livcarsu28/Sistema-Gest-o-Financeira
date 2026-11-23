package financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import financeiro.dto.ItemMovimentacaoRelatorioDTO;
import financeiro.dto.PublicacaoFinanceiraDTO;
import financeiro.dto.RelatorioAnualDTO;
import financeiro.dto.RelatorioMensalDTO;
import financeiro.dto.TotalizadorCategoriaDTO;
import financeiro.repository.RelatorioRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelatorioServiceTests {

    private RelatorioRepository repository;
    private RelatorioService service;

    @BeforeEach
    void setup() {
        repository = mock(RelatorioRepository.class);
        service = new RelatorioService(repository);
    }

    @Test
    void gerarRelatorioMensalCalculaTotaisESaldo() {
        when(repository.consultarTotaisPeriodo(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new RelatorioRepository.ResumoPeriodoRaw(
                        new BigDecimal("5000.00"),
                        new BigDecimal("3200.50"),
                        new BigDecimal("450.00"),
                        8L
                ));
        when(repository.listarMovimentacoesPeriodo(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        new ItemMovimentacaoRelatorioDTO(1, LocalDateTime.now(), "ENTRADA", new BigDecimal("5000.00"), "Doação", "Doações", "PAGO", "PIX")
                ));

        RelatorioMensalDTO relatorio = service.gerarRelatorioMensal(11, 2026);

        assertEquals(11, relatorio.mes());
        assertEquals(2026, relatorio.ano());
        assertEquals(new BigDecimal("5000.00"), relatorio.totalEntradas());
        assertEquals(new BigDecimal("3200.50"), relatorio.totalSaidas());
        assertEquals(new BigDecimal("1799.50"), relatorio.saldo());
        assertEquals(new BigDecimal("450.00"), relatorio.totalPendente());
        assertEquals(8L, relatorio.quantidadeMovimentacoes());
        assertEquals(1, relatorio.movimentacoes().size());
    }

    @Test
    void gerarRelatorioAnualConsolida12MesesECalculaMedia() {
        when(repository.consultarMesesAno(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        new RelatorioRepository.ItemMesConsolidadoRaw(1, new BigDecimal("1000.00"), new BigDecimal("600.00"), 4L),
                        new RelatorioRepository.ItemMesConsolidadoRaw(2, new BigDecimal("2000.00"), new BigDecimal("1800.00"), 6L)
                ));

        RelatorioAnualDTO anual = service.gerarRelatorioAnual(2026);

        assertEquals(2026, anual.ano());
        assertEquals(12, anual.meses().size());
        assertEquals(new BigDecimal("3000.00"), anual.totalEntradasAno());
        assertEquals(new BigDecimal("2400.00"), anual.totalSaidasAno());
        assertEquals(new BigDecimal("600.00"), anual.saldoAno());
        // 2400 / 12 = 200.00
        assertEquals(new BigDecimal("200.00"), anual.mediaMensalGastos());

        // Mês 1
        assertEquals("Janeiro", anual.meses().get(0).nomeMes());
        assertEquals(new BigDecimal("1000.00"), anual.meses().get(0).entradas());
        assertEquals(new BigDecimal("400.00"), anual.meses().get(0).saldo());

        // Mês 3 (sem movimentação no banco) deve estar zerado
        assertEquals("Março", anual.meses().get(2).nomeMes());
        assertEquals(BigDecimal.ZERO, anual.meses().get(2).entradas());
        assertEquals(BigDecimal.ZERO, anual.meses().get(2).saidas());
        assertEquals(BigDecimal.ZERO, anual.meses().get(2).saldo());
    }

    @Test
    void gerarTotalizadoresPorCategoriaCalculaPercentuais() {
        when(repository.consultarTotaisPorCategoria(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        new RelatorioRepository.TotalizadorCategoriaRaw(1, "Alimentação", "SAIDA", new BigDecimal("750.00"), 3L),
                        new RelatorioRepository.TotalizadorCategoriaRaw(2, "Educação", "SAIDA", new BigDecimal("250.00"), 1L)
                ));

        List<TotalizadorCategoriaDTO> categorias = service.gerarTotalizadoresPorCategoria(11, 2026);

        assertEquals(2, categorias.size());
        assertEquals("Alimentação", categorias.get(0).categoria());
        assertEquals(75.0, categorias.get(0).percentual());
        assertEquals("Educação", categorias.get(1).categoria());
        assertEquals(25.0, categorias.get(1).percentual());
    }

    @Test
    void publicarDadosNoSiteSalvaERetornaPublicacaoAtualizada() {
        when(repository.consultarTotaisPeriodo(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new RelatorioRepository.ResumoPeriodoRaw(
                        new BigDecimal("8000.00"), new BigDecimal("4500.00"), BigDecimal.ZERO, 10L));
        when(repository.listarMovimentacoesPeriodo(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        when(repository.salvarPublicacao(eq(11), eq(2026), any(BigDecimal.class), any(BigDecimal.class), eq("Atualizado"), any(), any()))
                .thenReturn(1);

        when(repository.consultarUltimaPublicacao()).thenReturn(Optional.of(
                new PublicacaoFinanceiraDTO(1, 11, 2026, null, new BigDecimal("4500.00"), new BigDecimal("8000.00"), LocalDateTime.now(), "Atualizado", "Aprovado", true)
        ));

        PublicacaoFinanceiraDTO pub = service.publicarDadosNoSite(11, 2026, "Aprovado", 1);

        assertTrue(pub.disponivel());
        assertEquals(11, pub.mesReferencia());
        assertEquals(new BigDecimal("4500.00"), pub.totalGastos());
        verify(repository).salvarPublicacao(eq(11), eq(2026), eq(new BigDecimal("4500.00")), eq(new BigDecimal("8000.00")), eq("Atualizado"), eq("Aprovado"), eq(1));
    }

    @Test
    void consultarUltimaPublicacaoRetornaNaoPublicadoQuandoVazio() {
        when(repository.consultarUltimaPublicacao()).thenReturn(Optional.empty());

        PublicacaoFinanceiraDTO pub = service.consultarUltimaPublicacao();

        assertNotNull(pub);
        assertFalse(pub.disponivel());
        assertEquals("Pendente", pub.statusAtualizacao());
    }
}
