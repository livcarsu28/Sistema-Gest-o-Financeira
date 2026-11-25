package financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import financeiro.dto.ItemMesRelatorioDTO;
import financeiro.dto.PublicacaoFinanceiraDTO;
import financeiro.dto.RelatorioAnualDTO;
import financeiro.dto.RelatorioMensalDTO;
import financeiro.dto.TotalizadorCategoriaDTO;
import financeiro.repository.RelatorioRepository;

@Service
public class RelatorioService {

    private static final Locale LOCALE_BRASIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATADOR_PERIODO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", LOCALE_BRASIL);

    private final RelatorioRepository relatorioRepository;

    public RelatorioService(RelatorioRepository relatorioRepository) {
        this.relatorioRepository = relatorioRepository;
    }

    public RelatorioMensalDTO gerarRelatorioMensal(int mes, int ano) {
        if (mes < 1 || mes > 12) {
            mes = LocalDate.now().getMonthValue();
        }
        if (ano < 2000 || ano > 2100) {
            ano = LocalDate.now().getYear();
        }

        LocalDate dataBase = LocalDate.of(ano, mes, 1);
        LocalDateTime inicio = dataBase.atStartOfDay();
        LocalDateTime fim = dataBase.plusMonths(1).atStartOfDay();

        var totais = relatorioRepository.consultarTotaisPeriodo(inicio, fim);
        var movimentacoes = relatorioRepository.listarMovimentacoesPeriodo(inicio, fim);

        BigDecimal saldo = totais.entradas().subtract(totais.saidas());
        String rawPeriodo = dataBase.format(FORMATADOR_PERIODO);
        String periodoFormatado = rawPeriodo.substring(0, 1).toUpperCase(LOCALE_BRASIL) + rawPeriodo.substring(1);

        return new RelatorioMensalDTO(
                mes,
                ano,
                periodoFormatado,
                totais.entradas(),
                totais.saidas(),
                saldo,
                totais.pendentes(),
                totais.quantidade(),
                movimentacoes
        );
    }

    public RelatorioAnualDTO gerarRelatorioAnual(int ano) {
        if (ano < 2000 || ano > 2100) {
            ano = LocalDate.now().getYear();
        }

        LocalDateTime inicioAno = LocalDate.of(ano, 1, 1).atStartOfDay();
        LocalDateTime fimAno = LocalDate.of(ano + 1, 1, 1).atStartOfDay();

        var dadosBanco = relatorioRepository.consultarMesesAno(inicioAno, fimAno);
        Map<Integer, RelatorioRepository.ItemMesConsolidadoRaw> mapaMeses = dadosBanco.stream()
                .collect(Collectors.toMap(RelatorioRepository.ItemMesConsolidadoRaw::mes, m -> m));

        List<ItemMesRelatorioDTO> listaMeses = new ArrayList<>();
        BigDecimal totalEntradasAno = BigDecimal.ZERO;
        BigDecimal totalSaidasAno = BigDecimal.ZERO;

        for (int m = 1; m <= 12; m++) {
            String nomeMes = Month.of(m).getDisplayName(TextStyle.FULL, LOCALE_BRASIL);
            // Capitaliza primeira letra do mês
            nomeMes = nomeMes.substring(0, 1).toUpperCase(LOCALE_BRASIL) + nomeMes.substring(1);

            var itemRaw = mapaMeses.get(m);
            BigDecimal entradas = (itemRaw != null) ? itemRaw.entradas() : BigDecimal.ZERO;
            BigDecimal saidas = (itemRaw != null) ? itemRaw.saidas() : BigDecimal.ZERO;
            long qtd = (itemRaw != null) ? itemRaw.quantidade() : 0L;
            BigDecimal saldo = entradas.subtract(saidas);

            totalEntradasAno = totalEntradasAno.add(entradas);
            totalSaidasAno = totalSaidasAno.add(saidas);

            listaMeses.add(new ItemMesRelatorioDTO(m, nomeMes, entradas, saidas, saldo, qtd));
        }

        BigDecimal saldoAno = totalEntradasAno.subtract(totalSaidasAno);
        BigDecimal mediaMensalGastos = totalSaidasAno.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        return new RelatorioAnualDTO(ano, totalEntradasAno, totalSaidasAno, saldoAno, mediaMensalGastos, listaMeses);
    }

    public List<TotalizadorCategoriaDTO> gerarTotalizadoresPorCategoria(int mes, int ano) {
        if (mes < 1 || mes > 12) {
            mes = LocalDate.now().getMonthValue();
        }
        if (ano < 2000 || ano > 2100) {
            ano = LocalDate.now().getYear();
        }

        LocalDate dataBase = LocalDate.of(ano, mes, 1);
        LocalDateTime inicio = dataBase.atStartOfDay();
        LocalDateTime fim = dataBase.plusMonths(1).atStartOfDay();

        var listaRaw = relatorioRepository.consultarTotaisPorCategoria(inicio, fim);

        BigDecimal somaTotal = listaRaw.stream()
                .map(RelatorioRepository.TotalizadorCategoriaRaw::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return listaRaw.stream().map(item -> {
            double percentual = 0.0;
            if (somaTotal.compareTo(BigDecimal.ZERO) > 0) {
                percentual = item.total()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(somaTotal, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            return new TotalizadorCategoriaDTO(
                    item.idCategoria(),
                    item.categoria(),
                    item.tipo(),
                    item.total(),
                    item.quantidade(),
                    percentual
            );
        }).toList();
    }

    public List<RelatorioRepository.CategoriaImpressao> categoriasImpressao() {
        return relatorioRepository.categoriasImpressao();
    }

    @Transactional
    public PublicacaoFinanceiraDTO publicarDadosNoSite(int mes, int ano, String observacao, Integer idUsuario) {
        LocalDateTime inicio = LocalDate.of(ano, mes, 1).atStartOfDay();
        var categorias = relatorioRepository.gastosPagosPorCategoria(inicio, inicio.plusMonths(1));
        BigDecimal gastos = categorias.stream().map(RelatorioRepository.GastoCategoria::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        RelatorioMensalDTO relatorio = gerarRelatorioMensal(mes, ano);
        relatorioRepository.salvarPublicacao(
                mes,
                ano,
                gastos,
                relatorio.totalEntradas(),
                "Atualizado",
                observacao,
                idUsuario
        );
        relatorioRepository.salvarCategoriasPublicadas(categorias);
        return consultarUltimaPublicacao();
    }

    @Transactional
    public void retirarPublicacao(Integer idUsuario) {
        var ultima = relatorioRepository.consultarUltimaPublicacao();
        if (ultima.isEmpty() || PublicacaoFinanceiraDTO.STATUS_RETIRADA.equals(ultima.get().statusAtualizacao())) {
            return;
        }
        var publicada = ultima.get();
        relatorioRepository.salvarPublicacao(publicada.mesReferencia(), publicada.anoReferencia(),
                BigDecimal.ZERO, BigDecimal.ZERO, PublicacaoFinanceiraDTO.STATUS_RETIRADA, null, idUsuario);
    }

    public PublicacaoFinanceiraDTO consultarUltimaPublicacao() {
        return relatorioRepository.consultarUltimaPublicacao()
                .map(pub -> {
                    if (PublicacaoFinanceiraDTO.STATUS_RETIRADA.equals(pub.statusAtualizacao())) {
                        return PublicacaoFinanceiraDTO.retirada();
                    }
                    LocalDate ref = LocalDate.of(pub.anoReferencia(), pub.mesReferencia(), 1);
                    String rawRef = ref.format(FORMATADOR_PERIODO);
                    String periodoFormatado = rawRef.substring(0, 1).toUpperCase(LOCALE_BRASIL) + rawRef.substring(1);
                    return new PublicacaoFinanceiraDTO(
                            pub.idPublicacao(),
                            pub.mesReferencia(),
                            pub.anoReferencia(),
                            periodoFormatado,
                            pub.totalGastos(),
                            pub.totalEntradas(),
                            pub.dataPublicacao(),
                            pub.statusAtualizacao(),
                            pub.observacao(),
                            true
                    );
                })
                .orElseGet(PublicacaoFinanceiraDTO::naoPublicado);
    }
}
