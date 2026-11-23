package financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioMensalDTO(
        int mes,
        int ano,
        String periodoFormatado,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal saldo,
        BigDecimal totalPendente,
        long quantidadeMovimentacoes,
        List<ItemMovimentacaoRelatorioDTO> movimentacoes
) {
}
