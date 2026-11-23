package financeiro.dto;

import java.math.BigDecimal;

public record ItemMesRelatorioDTO(
        int mes,
        String nomeMes,
        BigDecimal entradas,
        BigDecimal saidas,
        BigDecimal saldo,
        long quantidadeMovimentacoes
) {
}
