
package financeiro.controller;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import financeiro.service.DashboardService;
import financeiro.service.PixService;

@Controller
public class FinanceiroController {

    private static final Locale LOCALE_BRASIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATADOR_PERIODO =
            DateTimeFormatter.ofPattern("MMMM 'de' yyyy", LOCALE_BRASIL);

    private final DashboardService dashboardService;
    private final PixService pixService;

    public FinanceiroController(DashboardService dashboardService, PixService pixService) {
        this.dashboardService = dashboardService;
        this.pixService = pixService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var dados = dashboardService.consultarDashboard();
        var moeda = NumberFormat.getCurrencyInstance(LOCALE_BRASIL);
        model.addAttribute("periodoReferencia", dados.periodo().format(FORMATADOR_PERIODO));
        model.addAttribute("saldoFormatado", moeda.format(dados.saldo()));
        model.addAttribute("entradasFormatadas", moeda.format(dados.entradas()));
        model.addAttribute("saidasFormatadas", moeda.format(dados.saidas()));
        model.addAttribute("quantidadeMovimentacoes", dados.quantidadeMovimentacoes());
        model.addAttribute("pix", pixService.consultar());
        return "dashboard";
    }

    @GetMapping("/movimentacoes")
    public String movimentacoes() {
        return "movimentacoes";
    }

    @GetMapping("/relatorios")
    public String relatorios() {
        return "relatorios";
    }

     @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        model.addAttribute("pix", pixService.consultar());
        return "configuracoes";
    }
}
