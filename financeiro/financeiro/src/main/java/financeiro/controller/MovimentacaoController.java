
package financeiro.controller;

import financeiro.dto.MovimentacaoForm;
import financeiro.model.Movimentacao;
import financeiro.repository.CategoriaRepository;
import financeiro.repository.FormaPagamentoRepository;
import financeiro.service.CadastrosService;
import financeiro.service.MovimentacaoService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
@RequestMapping("/movimentacoes")
public class MovimentacaoController {

    private final MovimentacaoService service;
    private final CategoriaRepository categorias;
    private final FormaPagamentoRepository formas;
    private final CadastrosService cadastrosService;

    public MovimentacaoController(
            MovimentacaoService service,
            CategoriaRepository categorias,
            FormaPagamentoRepository formas,
            CadastrosService cadastrosService) {

        this.service = service;
        this.categorias = categorias;
        this.formas = formas;
        this.cadastrosService = cadastrosService;
    }

    // Tela principal
    @GetMapping
    public String inicio(Model model) {
        YearMonth mes = YearMonth.now();

        List<Movimentacao> movimentosDoMes = service.mes(mes);

        model.addAttribute("paginaAtiva", "movimentacoes");
        model.addAttribute("lista", service.recentes());
        model.addAttribute("mes", mes);

        model.addAttribute("categorias",
                categorias.findByAtivoTrueOrderByNomeAsc());

        model.addAttribute("formas",
                formas.findByAtivoTrueOrderByNomeAsc());

        model.addAttribute("entradas",
                totalPago(movimentosDoMes, "ENTRADA"));

        model.addAttribute("saidas",
                totalPago(movimentosDoMes, "SAIDA"));

        return "movimentacoes";
    }

    // API de categorias
    @GetMapping("/categorias")
    @ResponseBody
    public ResponseEntity<?> listarCategorias() {
        return ResponseEntity.ok(
                cadastrosService.listarCategorias());
    }

    // API de formas de pagamento
    @GetMapping("/formas-pagamento")
    @ResponseBody
    public ResponseEntity<?> listarFormasPagamento() {
        return ResponseEntity.ok(
                cadastrosService.listarFormasPagamento());
    }

    // Histórico de movimentações
    @GetMapping("/historico")
    public String historico(
            @RequestParam(required = false) String periodo,
            Model model) {

        YearMonth atual = YearMonth.now();
        YearMonth mes;

        try {
            mes = periodo == null
                    ? atual
                    : YearMonth.parse(periodo);

        } catch (DateTimeParseException e) {
            return "redirect:/movimentacoes/historico";
        }

        if (mes.isAfter(atual.plusMonths(3))
                || mes.isBefore(atual.minusMonths(3))) {
            return "redirect:/relatorios";
        }

        model.addAttribute("paginaAtiva", "movimentacoes");
        model.addAttribute("lista", service.mes(mes));
        model.addAttribute("mes", mes);

        model.addAttribute("periodos", List.of(
                atual.plusMonths(3),
                atual.plusMonths(2),
                atual.plusMonths(1),
                atual,
                atual.minusMonths(1),
                atual.minusMonths(2),
                atual.minusMonths(3)));

        return "historico";
    }

    @InitBinder
    void configurarFormulario(WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    // Cadastro de movimentações
    @PostMapping
    public String salvar(
            @Valid @ModelAttribute MovimentacaoForm form,
            BindingResult errors,
            RedirectAttributes flash) {

        if (errors.hasErrors()) {
            flash.addFlashAttribute(
                    "erro",
                    "Confira os campos obrigatórios.");
        } else {
            try {
                service.salvar(form);

                flash.addFlashAttribute(
                        "sucesso",
                        "Movimentação cadastrada.");

            } catch (RuntimeException e) {
                flash.addFlashAttribute(
                        "erro",
                        e.getMessage());
            }
        }

        return "redirect:/movimentacoes";
    }

    // Total de movimentações pagas
    private static BigDecimal totalPago(
            List<Movimentacao> lista,
            String tipo) {

        return lista.stream()
                .filter(m ->
                        tipo.equals(m.tipo)
                        && "PAGO".equals(m.statusPagamento)
                        && m.valor != null)
                .map(m -> m.valor)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add);
    }
}
