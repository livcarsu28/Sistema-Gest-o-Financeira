package financeiro.controller;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import financeiro.dto.PublicacaoFinanceiraDTO;
import financeiro.dto.PublicacaoRequest;
import financeiro.dto.RelatorioAnualDTO;
import financeiro.dto.RelatorioMensalDTO;
import financeiro.dto.TotalizadorCategoriaDTO;
import financeiro.security.UsuarioAutenticado;
import financeiro.service.RelatorioService;

@Controller
public class RelatorioController {

    private static final Locale LOCALE_BRASIL = Locale.forLanguageTag("pt-BR");
    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/relatorios")
    public String relatorios(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false, defaultValue = "mensal") String aba,
            Model model
    ) {
        LocalDate hoje = LocalDate.now();
        int mesSelecionado = (mes != null && mes >= 1 && mes <= 12) ? mes : hoje.getMonthValue();
        int anoSelecionado = (ano != null && ano >= 2000 && ano <= 2100) ? ano : hoje.getYear();

        RelatorioMensalDTO relatorioMensal = relatorioService.gerarRelatorioMensal(mesSelecionado, anoSelecionado);
        RelatorioAnualDTO relatorioAnual = relatorioService.gerarRelatorioAnual(anoSelecionado);
        List<TotalizadorCategoriaDTO> totalizadores = relatorioService.gerarTotalizadoresPorCategoria(mesSelecionado, anoSelecionado);
        PublicacaoFinanceiraDTO ultimaPublicacao = relatorioService.consultarUltimaPublicacao();

        NumberFormat moeda = NumberFormat.getCurrencyInstance(LOCALE_BRASIL);

        model.addAttribute("paginaAtiva", "relatorios");
        model.addAttribute("abaAtiva", aba);
        model.addAttribute("mesSelecionado", mesSelecionado);
        model.addAttribute("anoSelecionado", anoSelecionado);
        model.addAttribute("relatorioMensal", relatorioMensal);
        model.addAttribute("relatorioAnual", relatorioAnual);
        model.addAttribute("totalizadoresCategorias", totalizadores);
        model.addAttribute("ultimaPublicacao", ultimaPublicacao);

        // Formatações auxiliares para apresentação direta no template
        model.addAttribute("totalEntradasFormatado", moeda.format(relatorioMensal.totalEntradas()));
        model.addAttribute("totalSaidasFormatado", moeda.format(relatorioMensal.totalSaidas()));
        model.addAttribute("saldoFormatado", moeda.format(relatorioMensal.saldo()));
        model.addAttribute("totalPendenteFormatado", moeda.format(relatorioMensal.totalPendente()));
        model.addAttribute("totalEntradasAnoFormatado", moeda.format(relatorioAnual.totalEntradasAno()));
        model.addAttribute("totalSaidasAnoFormatado", moeda.format(relatorioAnual.totalSaidasAno()));
        model.addAttribute("saldoAnoFormatado", moeda.format(relatorioAnual.saldoAno()));
        model.addAttribute("mediaMensalGastosFormatada", moeda.format(relatorioAnual.mediaMensalGastos()));

        // Listas para os seletores
        List<Map<String, Object>> listaMeses = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, Object> item = new HashMap<>();
            String nome = Month.of(m).getDisplayName(TextStyle.FULL, LOCALE_BRASIL);
            nome = nome.substring(0, 1).toUpperCase(LOCALE_BRASIL) + nome.substring(1);
            item.put("numero", m);
            item.put("nome", nome);
            listaMeses.add(item);
        }
        model.addAttribute("mesesOpcoes", listaMeses);

        List<Integer> anosOpcoes = List.of(hoje.getYear() - 2, hoje.getYear() - 1, hoje.getYear(), hoje.getYear() + 1);
        model.addAttribute("anosOpcoes", anosOpcoes);

        return "relatorios";
    }

    // -------------------------------------------------------------------------
    // APIs REST para consulta assíncrona (troca de filtros sem recarregar a tela)
    // -------------------------------------------------------------------------

    @GetMapping("/relatorios/api/mensal")
    @ResponseBody
    public ResponseEntity<RelatorioMensalDTO> obterRelatorioMensal(
            @RequestParam int mes,
            @RequestParam int ano
    ) {
        return ResponseEntity.ok(relatorioService.gerarRelatorioMensal(mes, ano));
    }

    @GetMapping("/relatorios/api/anual")
    @ResponseBody
    public ResponseEntity<RelatorioAnualDTO> obterRelatorioAnual(@RequestParam int ano) {
        return ResponseEntity.ok(relatorioService.gerarRelatorioAnual(ano));
    }

    @GetMapping("/relatorios/api/categorias")
    @ResponseBody
    public ResponseEntity<List<TotalizadorCategoriaDTO>> obterCategorias(
            @RequestParam int mes,
            @RequestParam int ano
    ) {
        return ResponseEntity.ok(relatorioService.gerarTotalizadoresPorCategoria(mes, ano));
    }

    @PostMapping("/relatorios/api/publicar")
    @ResponseBody
    public ResponseEntity<PublicacaoFinanceiraDTO> publicarRelatorioNoSite(
            @RequestBody(required = false) PublicacaoRequest requestBody,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) String observacao,
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado
    ) {
        int mesFinal = (requestBody != null && requestBody.mes() > 0) ? requestBody.mes() : (mes != null ? mes : LocalDate.now().getMonthValue());
        int anoFinal = (requestBody != null && requestBody.ano() > 0) ? requestBody.ano() : (ano != null ? ano : LocalDate.now().getYear());
        String obsFinal = (requestBody != null && requestBody.observacao() != null) ? requestBody.observacao() : (observacao != null ? observacao : "");

        Integer idUsuario = (usuarioAutenticado != null) ? usuarioAutenticado.getId() : null;
        PublicacaoFinanceiraDTO publicada = relatorioService.publicarDadosNoSite(
                mesFinal,
                anoFinal,
                obsFinal,
                idUsuario
        );
        return ResponseEntity.ok(publicada);
    }

    // -------------------------------------------------------------------------
    // Endpoint público para integração com o site institucional (Cards de Transparência)
    // -------------------------------------------------------------------------

    @GetMapping("/api/publicacao-transparencia")
    @ResponseBody
    public ResponseEntity<PublicacaoFinanceiraDTO> obterPublicacaoTransparencia() {
        return ResponseEntity.ok(relatorioService.consultarUltimaPublicacao());
    }
}
