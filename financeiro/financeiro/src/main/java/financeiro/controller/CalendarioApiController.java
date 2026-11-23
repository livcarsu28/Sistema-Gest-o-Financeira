package financeiro.controller;

import financeiro.dto.AnotacaoRequest;
import financeiro.dto.LancamentoRequest;
import financeiro.model.Anotacao;
import financeiro.model.Categoria;
import financeiro.model.FormaPagamento;
import financeiro.model.Movimentacao;
import financeiro.repository.AnotacaoRepository;
import financeiro.repository.CategoriaRepository;
import financeiro.repository.FormaPagamentoRepository;
import financeiro.service.MovimentacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/financeiro")
public class CalendarioApiController {
    private final MovimentacaoService movimentacoes;
    private final AnotacaoRepository anotacoes;
    private final CategoriaRepository categorias;
    private final FormaPagamentoRepository formas;

    public CalendarioApiController(MovimentacaoService movimentacoes, AnotacaoRepository anotacoes,
                                  CategoriaRepository categorias, FormaPagamentoRepository formas) {
        this.movimentacoes = movimentacoes;
        this.anotacoes = anotacoes;
        this.categorias = categorias;
        this.formas = formas;
    }

    @GetMapping
    public Estado estado() {
        List<Movimento> movs = movimentacoes.listar().stream().map(Movimento::de).toList();
        List<Nota> notas = anotacoes.findAll().stream().map(Nota::de).toList();
        return new Estado(LocalDate.now().toString(), movs, notas, categorias.findAll(), formas.findAll());
    }

    @PostMapping("/movimentacoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void criar(@Valid @RequestBody LancamentoRequest dados, @RequestParam(required = false) Integer anotacaoId) {
        Anotacao origem = anotacaoId == null ? null : buscarNota(anotacaoId);
        movimentacoes.criar(dados);
        if (origem != null) origem.concluido = true;
    }

    @PutMapping("/movimentacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void atualizar(@PathVariable Integer id, @Valid @RequestBody LancamentoRequest dados,
                          @RequestParam(defaultValue = "false") boolean proximas) {
        movimentacoes.atualizar(id, dados, proximas);
    }

    @PatchMapping("/movimentacoes/{id}/pagamento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pagamento(@PathVariable Integer id, @RequestParam boolean pago) {
        movimentacoes.definirPagamento(id, pago);
    }

    @DeleteMapping("/movimentacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean proximas) {
        movimentacoes.remover(id, proximas);
    }

    @PostMapping("/anotacoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void criarNota(@Valid @RequestBody AnotacaoRequest dados) {
        if (dados.categoriaId() != null && !categorias.existsById(dados.categoriaId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria inválida.");
        }
        Anotacao nota = new Anotacao();
        nota.descricao = dados.descricao().trim();
        nota.dataCriacao = LocalDateTime.now();
        nota.dataLembrete = dados.data().atStartOfDay();
        nota.categoriaId = dados.categoriaId();
        nota.usuarioId = movimentacoes.usuarioDeCadastro();
        anotacoes.save(nota);
    }

    @PatchMapping("/anotacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void concluirNota(@PathVariable Integer id, @RequestParam boolean concluida) {
        buscarNota(id).concluido = concluida;
    }

    @DeleteMapping("/anotacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void removerNota(@PathVariable Integer id) {
        anotacoes.delete(buscarNota(id));
    }

    private Anotacao buscarNota(Integer id) {
        return anotacoes.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Anotação não encontrada."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> erro(ResponseStatusException e) {
        return org.springframework.http.ResponseEntity.status(e.getStatusCode())
            .body(Map.of("mensagem", Objects.toString(e.getReason(), "Operação indisponível.")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> camposInvalidos() {
        return Map.of("mensagem", "Confira a data, a descrição, a categoria e o valor informado.");
    }

    public record Estado(String hoje, List<Movimento> movs, List<Nota> notas,
                         List<Categoria> categorias, List<FormaPagamento> formas) {}

    public record Movimento(Integer id, String tipo, String descricao, Integer categoriaId, BigDecimal valor,
                            Integer formaPagamentoId, String data, String status, String serie,
                            boolean recorrente, boolean semanal, int parcelas, int parcelaAtual, String obs) {
        static Movimento de(Movimentacao m) {
            return new Movimento(m.id, m.tipo.toLowerCase(Locale.ROOT), Objects.toString(m.descricao, ""),
                m.categoriaId, m.valor, m.formaPagamentoId, MovimentacaoService.dataEfetiva(m).toString(),
                "PAGO".equals(m.statusPagamento) ? "pago" : "pendente", m.serie,
                Boolean.TRUE.equals(m.recorrente), "SEMANAL".equals(m.tipoRecorrencia),
                Objects.requireNonNullElse(m.numeroParcelas, 1), Objects.requireNonNullElse(m.parcelaAtual, 1), m.observacao);
        }
    }

    public record Nota(Integer id, String descricao, String data, Integer categoriaId, boolean concluida) {
        static Nota de(Anotacao a) {
            LocalDateTime data = a.dataLembrete == null ? a.dataCriacao : a.dataLembrete;
            return new Nota(a.id, a.descricao, data.toLocalDate().toString(), a.categoriaId, a.concluido);
        }
    }
}
