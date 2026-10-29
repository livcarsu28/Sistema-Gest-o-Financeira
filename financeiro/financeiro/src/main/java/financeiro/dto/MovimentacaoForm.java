    package financeiro.dto;
    import jakarta.validation.constraints.*;
    import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
    import org.springframework.format.annotation.DateTimeFormat;

    public class MovimentacaoForm {

        @NotBlank @Pattern(regexp="ENTRADA|SAIDA") 
        public String tipo;

        @NotNull @DecimalMin("0.01")
        public BigDecimal valor;

        @NotBlank @Size(max=500)
        public String descricao;

        @NotNull 
        public Integer categoriaId;

        @NotNull 
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
        public LocalDate data;

        public Integer formaPagamentoId;

        public boolean recorrente;
        @Pattern(regexp = "MENSAL|SEMANAL")
public String tipoRecorrencia;

@Min(1)
@Max(31)
public Integer diaMes;

public List<DayOfWeek> diasSemana;

        @Size(max=30) 
        public String conta;
    }