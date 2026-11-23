package financeiro.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record AnotacaoRequest(
    @NotBlank @Size(max = 255) String descricao,
    @NotNull LocalDate data,
    Integer categoriaId
) {}
