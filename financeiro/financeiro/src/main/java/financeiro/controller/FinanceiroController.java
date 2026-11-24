
package financeiro.controller;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import financeiro.repository.InstituicaoRepository;
import financeiro.repository.InstituicaoRepository.Instituicao;
import financeiro.service.DashboardService;
import financeiro.service.PixService;

@Controller
public class FinanceiroController {

    private static final Locale LOCALE_BRASIL =
            Locale.forLanguageTag("pt-BR");

    private static final DateTimeFormatter FORMATADOR_PERIODO =
            DateTimeFormatter.ofPattern(
                    "MMMM 'de' yyyy", LOCALE_BRASIL);

    private final DashboardService dashboardService;
    private final PixService pixService;
    private final InstituicaoRepository instituicaoRepository;

    public FinanceiroController(
            DashboardService dashboardService,
            PixService pixService,
            @Autowired(required = false)
            InstituicaoRepository instituicaoRepository) {

        this.dashboardService = dashboardService;
        this.pixService = pixService;
        this.instituicaoRepository = instituicaoRepository;
    }

    // Página inicial
        @GetMapping("/")
        public String inicio() {
        return "redirect:/dashboard";
        }

    // Login
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Dashboard
    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        var dados = dashboardService.consultarDashboard();

        var moeda = NumberFormat.getCurrencyInstance(
                LOCALE_BRASIL);

        model.addAttribute(
                "paginaAtiva", "dashboard");

        model.addAttribute(
                "periodoReferencia",
                dados.periodo().format(FORMATADOR_PERIODO));

        model.addAttribute(
                "saldoFormatado",
                moeda.format(dados.saldo()));

        model.addAttribute(
                "entradasFormatadas",
                moeda.format(dados.entradas()));

        model.addAttribute(
                "saidasFormatadas",
                moeda.format(dados.saidas()));

        model.addAttribute(
                "quantidadeMovimentacoes",
                dados.quantidadeMovimentacoes());

        model.addAttribute(
                "pix",
                pixService.consultar());

        return "dashboard";
    }

    // Calendário financeiro
    @GetMapping("/calendario")
    public String calendario(Model model) {

        model.addAttribute(
                "paginaAtiva", "calendario");

        return "calendario";
    }

    // Configurações
    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {

        model.addAttribute(
                "paginaAtiva", "configuracoes");

        model.addAttribute(
                "pix", pixService.consultar());

        Instituicao instituicao = null;

        if (instituicaoRepository != null) {

            var lista =
                    instituicaoRepository.consultar(false);

            if (!lista.isEmpty()) {
                instituicao = lista.get(0);
            }
        }

        if (instituicao == null) {

            instituicao = new Instituicao(
                    1,
                    "Não cadastrada",
                    "Rua não informada",
                    "S/N",
                    "Bairro",
                    "Cidade",
                    "UF",
                    "00000-000");
        }

        model.addAttribute(
                "instituicao", instituicao);

        return "configuracoes";
    }
}
