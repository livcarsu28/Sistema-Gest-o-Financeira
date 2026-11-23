package financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioAnualDTO(
        int ano,
        BigDecimal totalEntradasAno,
        BigDecimal totalSaidasAno,
        BigDecimal saldoAno,
        BigDecimal mediaMensalGastos,
        List<ItemMesRelatorioDTO> meses
) {
}
