package financeiro.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record LancamentoRequest(
    @NotBlank @Pattern(regexp = "entrada|saida") String tipo,
    @NotBlank @Size(max = 500) String descricao,
    @NotNull Integer categoriaId,
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal valor,
    Integer formaPagamentoId,
    @Size(max = 30) String obs,
    @NotNull LocalDate data,
    @NotBlank @Pattern(regexp = "pago|pendente") String status,
    @Pattern(regexp = "unica|mensal|semanal|parcelada") String repeticao,
    @Min(2) @Max(60) Integer parcelas,
    @Min(1) @Max(31) Integer diaMes,
    List<@NotNull DayOfWeek> diasSemana
) {}
