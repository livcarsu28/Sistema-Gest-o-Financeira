package financeiro.service;

import financeiro.dto.LancamentoRequest;
import financeiro.dto.MovimentacaoForm;
import financeiro.model.Movimentacao;
import financeiro.repository.CategoriaRepository;
import financeiro.repository.FormaPagamentoRepository;
import financeiro.repository.MovimentacaoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MovimentacaoService {
    private final MovimentacaoRepository repo;
    private final CategoriaRepository categorias;
    private final FormaPagamentoRepository formas;
    private final int devUserId;

    public MovimentacaoService(MovimentacaoRepository repo, CategoriaRepository categorias,
                              FormaPagamentoRepository formas,
                              @Value("${app.dev-user-id:${DEV_USER_ID:0}}") int devUserId) {
        this.repo = repo;
        this.categorias = categorias;
        this.formas = formas;
        this.devUserId = devUserId;
    }

    public List<Movimentacao> mes(YearMonth mes) {
        return repo.findByExcluidaFalseAndDataGreaterThanEqualAndDataLessThanOrderByDataDesc(
            mes.atDay(1).atStartOfDay(), mes.plusMonths(1).atDay(1).atStartOfDay());
    }

    public List<Movimentacao> recentes() {
        return repo.findTop5ByExcluidaFalseAndDataLessThanOrderByDataDescIdDesc(LocalDate.now().plusDays(1).atStartOfDay());
    }

    public int usuarioDeCadastro() {
        if (devUserId <= 0) throw invalido("Configure DEV_USER_ID com um id_usuario existente antes de cadastrar.");
        return devUserId;
    }

    @Transactional
    public void salvar(MovimentacaoForm form) {
        String repeticao = form.recorrente && form.tipoRecorrencia != null
            ? form.tipoRecorrencia.toLowerCase(Locale.ROOT) : "unica";
        if (form.recorrente && form.tipoRecorrencia == null) throw invalido("Selecione o tipo de recorrência.");
        criar(new LancamentoRequest(form.tipo.toLowerCase(Locale.ROOT), form.descricao, form.categoriaId,
            form.valor, form.formaPagamentoId, form.observacoes, form.data, "pago", repeticao, null,
            form.diaMes, form.diasSemana), form.conta);
    }

    @Transactional
    public void criar(LancamentoRequest dados) {
        criar(dados, null);
    }

    private void criar(LancamentoRequest dados, String conta) {
        validarReferencias(dados);
        Movimentacao primeira = new Movimentacao();
        primeira.usuarioId = usuarioDeCadastro();
        primeira.conta = conta;
        aplicarDados(primeira, dados);
        aplicarData(primeira, dados.data());
        aplicarStatus(primeira, dados.status().equals("pago"));
        switch (Objects.toString(dados.repeticao(), "unica")) {
            case "mensal" -> {
                primeira.recorrente = true;
                primeira.tipoRecorrencia = "MENSAL";
                primeira.diaMes = dados.diaMes() == null ? dados.data().getDayOfMonth() : dados.diaMes();
            }
            case "semanal" -> {
                if (dados.diasSemana() == null || dados.diasSemana().isEmpty()) {
                    throw invalido("Selecione pelo menos um dia da semana.");
                }
                primeira.recorrente = true;
                primeira.tipoRecorrencia = "SEMANAL";
                primeira.diasSemana = diasTexto(dados.diasSemana());
            }
            case "parcelada" -> {
                if (dados.parcelas() == null) throw invalido("Informe o número de parcelas.");
                primeira.numeroParcelas = dados.parcelas();
                primeira.diaMes = dados.data().getDayOfMonth();
            }
            default -> { }
        }

        if (primeira.recorrente || primeira.numeroParcelas > 1) {
            primeira.serie = UUID.randomUUID().toString();
            primeira.ordemSerie = 1;
            primeira.dataProgramada = dados.data();
        }
        repo.save(primeira);
        if (primeira.serie != null) completar(primeira, LocalDate.now().plusMonths(12));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Movimentacao> listar() {
        LocalDate limite = LocalDate.now().plusMonths(12);
        for (String serie : repo.seriesAtivas()) {
            Movimentacao origem = repo.bloquearSerie(serie).orElseThrow();
            if (!origem.serieEncerrada) completar(repo.findFirstBySerieOrderByOrdemSerieDesc(serie), limite);
        }
        return repo.findByExcluidaFalseOrderByDataDescIdDesc();
    }

    private void completar(Movimentacao ultima, LocalDate limite) {
        LocalDate proxima = proximaData(ultima);
        while (ultima.recorrente ? !proxima.isAfter(limite) : ultima.ordemSerie < ultima.numeroParcelas) {
            Movimentacao nova = new Movimentacao();
            nova.tipo = ultima.tipo;
            nova.valor = ultima.valor;
            nova.descricao = ultima.descricao;
            nova.conta = ultima.conta;
            nova.observacao = ultima.observacao;
            nova.categoriaId = ultima.categoriaId;
            nova.formaPagamentoId = ultima.formaPagamentoId;
            nova.usuarioId = ultima.usuarioId;
            nova.serie = ultima.serie;
            nova.ordemSerie = ultima.ordemSerie + 1;
            nova.recorrente = ultima.recorrente;
            nova.tipoRecorrencia = ultima.tipoRecorrencia;
            nova.diaMes = ultima.diaMes;
            nova.diasSemana = ultima.diasSemana;
            nova.numeroParcelas = ultima.numeroParcelas;
            nova.parcelaAtual = nova.numeroParcelas > 1 ? nova.ordemSerie : 1;
            nova.dataProgramada = proxima;
            aplicarData(nova, proxima);
            aplicarStatus(nova, false);
            repo.save(nova);
            ultima = nova;
            proxima = proximaData(ultima);
        }
    }

    private static LocalDate proximaData(Movimentacao m) {
        if ("SEMANAL".equals(m.tipoRecorrencia)) {
            List<DayOfWeek> dias = lerDias(m.diasSemana);
            LocalDate dia = m.dataProgramada.plusDays(1);
            while (!dias.contains(dia.getDayOfWeek())) dia = dia.plusDays(1);
            return dia;
        }
        YearMonth mes = YearMonth.from(m.dataProgramada).plusMonths(1);
        return mes.atDay(Math.min(m.diaMes, mes.lengthOfMonth()));
    }

    @Transactional
    public void atualizar(Integer id, LancamentoRequest dados, boolean proximas) {
        validarReferencias(dados);
        List<Movimentacao> grupo = bloquearGrupo(id);
        Movimentacao atual = localizar(grupo, id);
        if (!proximas && atual.serie != null && atual.recorrente && !grupo.get(0).serieEncerrada
            && grupo.get(grupo.size() - 1) == atual) {
            // Guarda a próxima ocorrência com os dados da série antes de editar a última exibida.
            completar(atual, proximaData(atual));
        }
        LocalDate antiga = dataEfetiva(atual);
        aplicarDados(atual, dados);
        if (!antiga.equals(dados.data())) aplicarData(atual, dados.data());
        aplicarStatus(atual, dados.status().equals("pago"));

        if (!proximas || atual.serie == null) return;
        boolean mudouAgenda = !antiga.equals(dados.data()) || !atual.dataProgramada.equals(dados.data());
        if (mudouAgenda) {
            if ("SEMANAL".equals(atual.tipoRecorrencia)) {
                long deslocamento = ChronoUnit.DAYS.between(atual.dataProgramada, dados.data());
                atual.diasSemana = diasTexto(lerDias(atual.diasSemana).stream().map(dia -> dia.plus(deslocamento)).toList());
            } else {
                atual.diaMes = dados.data().getDayOfMonth();
            }
            atual.dataProgramada = dados.data();
        }

        Movimentacao anterior = atual;
        for (Movimentacao item : grupo) {
            if (item.ordemSerie <= atual.ordemSerie) continue;
            aplicarDados(item, dados);
            if (mudouAgenda) {
                item.dataProgramada = proximaData(anterior);
                item.diaMes = atual.diaMes;
                item.diasSemana = atual.diasSemana;
                aplicarData(item, item.dataProgramada);
            }
            anterior = item;
        }
    }

    @Transactional
    public void definirPagamento(Integer id, boolean pago) {
        aplicarStatus(localizar(bloquearGrupo(id), id), pago);
    }

    @Transactional
    public void remover(Integer id, boolean proximas) {
        List<Movimentacao> grupo = bloquearGrupo(id);
        Movimentacao atual = localizar(grupo, id);
        if (atual.serie == null) {
            repo.delete(atual);
            return;
        }
        atual.excluida = true;
        if (proximas) {
            grupo.get(0).serieEncerrada = true;
            grupo.stream().filter(m -> m.ordemSerie > atual.ordemSerie).forEach(m -> m.excluida = true);
        }
    }

    private List<Movimentacao> bloquearGrupo(Integer id) {
        String serie = repo.serieDe(id);
        if (serie == null) return List.of(repo.bloquear(id).orElseThrow(MovimentacaoService::naoEncontrada));
        repo.bloquearSerie(serie).orElseThrow(MovimentacaoService::naoEncontrada);
        return repo.bloquearOcorrencias(serie);
    }

    private static Movimentacao localizar(List<Movimentacao> grupo, Integer id) {
        return grupo.stream().filter(m -> m.id.equals(id) && !m.excluida)
            .findFirst().orElseThrow(MovimentacaoService::naoEncontrada);
    }

    private void validarReferencias(LancamentoRequest dados) {
        if (!categorias.existsById(dados.categoriaId())) throw invalido("Categoria inválida.");
        if (dados.formaPagamentoId() != null && !formas.existsById(dados.formaPagamentoId())) {
            throw invalido("Forma de pagamento inválida.");
        }
    }

    private static void aplicarDados(Movimentacao m, LancamentoRequest dados) {
        m.tipo = dados.tipo().toUpperCase(Locale.ROOT);
        m.descricao = dados.descricao().trim();
        m.valor = dados.valor();
        m.categoriaId = dados.categoriaId();
        m.formaPagamentoId = dados.formaPagamentoId();
        m.observacao = dados.obs();
    }

    public static LocalDate dataEfetiva(Movimentacao m) {
        return m.dataVencimento == null ? m.data.toLocalDate() : m.dataVencimento;
    }

    private static void aplicarData(Movimentacao m, LocalDate data) {
        m.data = data.atStartOfDay();
        m.dataVencimento = data;
    }

    private static void aplicarStatus(Movimentacao m, boolean pago) {
        m.statusPagamento = pago ? "PAGO" : "PENDENTE";
        if (!pago) m.dataPagamento = null;
        else if (m.dataPagamento == null) m.dataPagamento = LocalDateTime.now();
    }

    private static List<DayOfWeek> lerDias(String dias) {
        return Arrays.stream(dias.split(",")).map(DayOfWeek::valueOf).toList();
    }

    private static String diasTexto(List<DayOfWeek> dias) {
        return dias.stream().distinct().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

    private static ResponseStatusException invalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private static ResponseStatusException naoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Movimentação não encontrada.");
    }
}
