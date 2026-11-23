package financeiro.controller;

<<<<<<< HEAD
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import financeiro.service.CadastrosService;

/**
 * Controller de movimentações.
 *
 * Fase atual: apenas os endpoints de consulta dos cadastros-base (categorias e
 * formas de pagamento) necessários para popular os formulários de lançamento.
 *
 * O CRUD de movimentações será implementado na próxima etapa.
 */
@Controller
@RequestMapping("/movimentacoes")
public class MovimentacaoController {

    private final CadastrosService cadastrosService;

    public MovimentacaoController(CadastrosService cadastrosService) {
        this.cadastrosService = cadastrosService;
    }

    // -------------------------------------------------------------------------
    // Tela principal de movimentações
    // -------------------------------------------------------------------------

    @GetMapping
    public String movimentacoes() {
        return "movimentacoes";
    }

    // -------------------------------------------------------------------------
    // APIs de suporte aos formulários — retornam JSON
    // -------------------------------------------------------------------------

    /**
     * GET /movimentacoes/categorias
     *
     * Retorna a lista de categorias de movimentação ativas, ordenadas por nome.
     * Usado pelos formulários de novo lançamento para popular o select de categoria.
     * Requer autenticação (protegido pelo SecurityConfig).
     */
    @GetMapping("/categorias")
    @ResponseBody
    public ResponseEntity<?> listarCategorias() {
        return ResponseEntity.ok(cadastrosService.listarCategorias());
    }

    /**
     * GET /movimentacoes/formas-pagamento
     *
     * Retorna a lista de formas de pagamento ativas, ordenadas por nome.
     * Usado pelos formulários de novo lançamento para popular o select de forma de pagamento.
     * O campo id_forma_pagamento é nullable em movimentacao, portanto este endpoint
     * pode retornar lista vazia sem impedir o lançamento.
     * Requer autenticação (protegido pelo SecurityConfig).
     */
    @GetMapping("/formas-pagamento")
    @ResponseBody
    public ResponseEntity<?> listarFormasPagamento() {
        return ResponseEntity.ok(cadastrosService.listarFormasPagamento());
=======
import financeiro.dto.MovimentacaoForm;
import financeiro.model.Movimentacao;
import financeiro.repository.CategoriaRepository;
import financeiro.repository.FormaPagamentoRepository;
import financeiro.service.MovimentacaoService;
import jakarta.validation.Valid;
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
public class MovimentacaoController {
    private final MovimentacaoService service;
    private final CategoriaRepository categorias;
    private final FormaPagamentoRepository formas;

    public MovimentacaoController(MovimentacaoService service, CategoriaRepository categorias,
                                  FormaPagamentoRepository formas) {
        this.service = service;
        this.categorias = categorias;
        this.formas = formas;
    }

    @GetMapping("/movimentacoes")
    public String inicio(Model model) {
        YearMonth mes = YearMonth.now();
        List<Movimentacao> movimentosDoMes = service.mes(mes);
        model.addAttribute("paginaAtiva", "movimentacoes");
        model.addAttribute("lista", service.recentes());
        model.addAttribute("mes", mes);
        model.addAttribute("categorias", categorias.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("formas", formas.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("entradas", totalPago(movimentosDoMes, "ENTRADA"));
        model.addAttribute("saidas", totalPago(movimentosDoMes, "SAIDA"));
        return "movimentacoes";
    }

    @GetMapping("/movimentacoes/historico")
    public String historico(@RequestParam(required = false) String periodo, Model model) {
        YearMonth atual = YearMonth.now();
        YearMonth mes;
        try {
            mes = periodo == null ? atual : YearMonth.parse(periodo);
        } catch (DateTimeParseException e) {
            return "redirect:/movimentacoes/historico";
        }
        if (mes.isAfter(atual) || mes.isBefore(atual.minusMonths(3))) return "redirect:/relatorios";

        model.addAttribute("paginaAtiva", "movimentacoes");
        model.addAttribute("lista", service.mes(mes));
        model.addAttribute("mes", mes);
        model.addAttribute("periodos", List.of(atual, atual.minusMonths(1), atual.minusMonths(2), atual.minusMonths(3)));
        return "historico";
    }

    @InitBinder
    void configurarFormulario(WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    @PostMapping("/movimentacoes")
    public String salvar(@Valid @ModelAttribute MovimentacaoForm form, BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("erro", "Confira os campos obrigatórios.");
        } else {
            try {
                service.salvar(form);
                flash.addFlashAttribute("sucesso", "Movimentação cadastrada.");
            } catch (RuntimeException e) {
                flash.addFlashAttribute("erro", e.getMessage());
            }
        }
        return "redirect:/movimentacoes";
    }

    private static BigDecimal totalPago(List<Movimentacao> lista, String tipo) {
        return lista.stream()
            .filter(m -> tipo.equals(m.tipo) && "PAGO".equals(m.statusPagamento) && m.valor != null)
            .map(m -> m.valor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
>>>>>>> movimentacoes
    }
}
