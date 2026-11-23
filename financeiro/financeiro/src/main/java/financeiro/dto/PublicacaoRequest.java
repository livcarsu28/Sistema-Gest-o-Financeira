package financeiro.dto;

public record PublicacaoRequest(
        int mes,
        int ano,
        String observacao
) {
}
