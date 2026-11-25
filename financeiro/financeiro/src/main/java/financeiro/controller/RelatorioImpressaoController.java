package financeiro.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import financeiro.repository.RelatorioRepository;

@Controller
public class RelatorioImpressaoController {
    private final RelatorioRepository repository;

    public RelatorioImpressaoController(RelatorioRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/relatorios/imprimir")
    public String imprimir(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                           @RequestParam(defaultValue = "TODOS") String tipo,
                           @RequestParam(defaultValue = "0") int categoria,
                           @RequestParam(defaultValue = "detalhado") String formato,
                           Model model) {
        if (fim.isBefore(inicio) || inicio.getYear() < 1900 || fim.getYear() > 2100
                || categoria < 0 || !Set.of("TODOS", "ENTRADA", "SAIDA").contains(tipo)
                || !Set.of("detalhado", "categoria", "resumo").contains(formato)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros de impressão inválidos.");
        }
        var linhas = repository.linhasImpressao(inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay(), tipo, categoria);
        Map<String, Totais> grupos = new TreeMap<>();
        for (var linha : linhas) {
            BigDecimal zero = BigDecimal.ZERO;
            Totais movimento = switch (linha.tipo()) {
                case "ENTRADA" -> new Totais(linha.valor(), zero);
                case "SAIDA" -> new Totais(zero, linha.valor());
                default -> throw new IllegalStateException("Tipo de movimentação inválido.");
            };
            grupos.merge(linha.categoria(), movimento, Totais::somar);
        }
        Totais totais = grupos.values().stream()
                .reduce(new Totais(BigDecimal.ZERO, BigDecimal.ZERO), Totais::somar);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("tipo", tipo);
        model.addAttribute("formato", formato);
        model.addAttribute("grupos", grupos);
        model.addAttribute("totais", totais);
        model.addAttribute("linhas", "detalhado".equals(formato) ? linhas : List.of());
        model.addAttribute("quantidade", linhas.size());
        model.addAttribute("categoriaNome", categoria == 0 ? "Todas" : repository.categoriasImpressao().stream()
                .filter(c -> c.id() == categoria).map(RelatorioRepository.CategoriaImpressao::nome)
                .findFirst().orElse("Categoria não encontrada"));
        return "relatorio-impressao";
    }

    public record Totais(BigDecimal entradas, BigDecimal saidas) {
        public Totais somar(Totais outro) {
            return new Totais(entradas.add(outro.entradas), saidas.add(outro.saidas));
        }
    }
}
