package financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicacaoFinanceiraDTO(
        Integer idPublicacao,
        int mesReferencia,
        int anoReferencia,
        String periodoFormatado,
        BigDecimal totalGastos,
        BigDecimal totalEntradas,
        LocalDateTime dataPublicacao,
        String statusAtualizacao,
        String observacao,
        boolean disponivel
) {
    public static PublicacaoFinanceiraDTO naoPublicado() {
        return new PublicacaoFinanceiraDTO(null, 0, 0, "Nenhuma publicação realizada",
                BigDecimal.ZERO, BigDecimal.ZERO, null, "Pendente", null, false);
    }
}
