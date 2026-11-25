package financeiro.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import financeiro.dto.PublicacaoFinanceiraDTO;
import financeiro.service.PixService;
import financeiro.service.RelatorioService;

@RestController
@CrossOrigin(origins = {
    "https://resgatedevidascriancafeliz.ong.br",
    "https://www.resgatedevidascriancafeliz.ong.br"
})
public class SitePublicoController {

    private final PixService pixService;
    private final RelatorioService relatorioService;
    private final String imagemUrl;
    private final JdbcTemplate jdbc;

    public SitePublicoController(PixService pixService, RelatorioService relatorioService,
                                 JdbcTemplate jdbc,
                                 @Value("${SITE_IMAGEM_PUBLICA_URL:}") String imagemUrl) {
        this.pixService = pixService;
        this.relatorioService = relatorioService;
        this.jdbc = jdbc;
        this.imagemUrl = urlHttpsPublica(imagemUrl);
    }

    @GetMapping("/api/site-publico")
    public ResponseEntity<DadosPublicos> consultar() {
        var ultima = relatorioService.consultarUltimaPublicacao();
        var publicacao = new ResumoPublicado(ultima.disponivel(), ultima.periodoFormatado(),
                ultima.totalGastos(), ultima.statusAtualizacao());

        var pix = pixService.consultar();
        String chave = pix.disponivel() ? pix.chave() : null;

        List<GastoPublicado> gastos = ultima.disponivel()
                ? jdbc.query("""
                        SELECT categoria, descricao, total
                        FROM publicacao_gasto_categoria
                        WHERE id_publicacao = ?
                        ORDER BY total DESC, categoria ASC
                        """,
                        (rs, row) -> new GastoPublicado(rs.getString("categoria"), rs.getString("descricao"), rs.getBigDecimal("total")),
                        ultima.idPublicacao())
                : List.of();

        // Se uma edição concorrente ocorreu durante a publicação, preserve o resumo
        // e não associe a ele um detalhamento que não fecha com o valor publicado.
        if (ultima.disponivel() && gastos.stream().map(GastoPublicado::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(ultima.totalGastos()) != 0) {
            gastos = List.of();
        }

        String imagemPublicada = PublicacaoFinanceiraDTO.STATUS_RETIRADA.equals(ultima.statusAtualizacao())
                ? null : imagemUrl;
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new DadosPublicos(publicacao, chave, imagemPublicada, gastos, pix.temporariamenteIndisponivel()));
    }

    private static String urlHttpsPublica(String valor) {
        try {
            URI uri = URI.create(valor.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null && uri.getUserInfo() == null ? uri.toString() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public record DadosPublicos(ResumoPublicado publicacao, String chavePix, String imagemUrl,
                               List<GastoPublicado> gastosPorCategoria, boolean pixTemporariamenteIndisponivel) {}

    public record GastoPublicado(String categoria, String descricao, BigDecimal total) {}

    public record ResumoPublicado(boolean disponivel, String periodoFormatado,
                                  BigDecimal totalGastos, String statusAtualizacao) {}
}
