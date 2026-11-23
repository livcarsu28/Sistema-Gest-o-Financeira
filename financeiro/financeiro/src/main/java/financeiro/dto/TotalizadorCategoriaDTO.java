package financeiro.dto;

import java.math.BigDecimal;

public record TotalizadorCategoriaDTO(
        int idCategoria,
        String categoria,
        String tipo,
        BigDecimal totalValor,
        long quantidade,
        double percentual
) {
}
