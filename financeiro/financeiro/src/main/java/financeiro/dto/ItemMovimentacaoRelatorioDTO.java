package financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemMovimentacaoRelatorioDTO(
        int idMovimentacao,
        LocalDateTime data,
        String tipo,
        BigDecimal valor,
        String descricao,
        String categoria,
        String statusPagamento,
        String formaPagamento
) {
}
