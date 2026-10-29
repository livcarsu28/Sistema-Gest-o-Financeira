package financeiro.controller;
import financeiro.dto.MovimentacaoForm;
import financeiro.model.Movimentacao;
import financeiro.repository.*;
import financeiro.service.MovimentacaoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Controller public class MovimentacaoController {
    private final MovimentacaoService service;
    private final CategoriaRepository categorias;
    private final FormaPagamentoRepository formas;

    public MovimentacaoController(MovimentacaoService s,CategoriaRepository c,FormaPagamentoRepository f){
        service=s;categorias=c;formas=f;
    }

    private void dados(Model model,List<Movimentacao> lista){
        model.addAttribute("lista",lista);
        model.addAttribute("categorias",categorias.findByAtivoTrueOrderByNomeAsc());model.addAttribute("formas",formas.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("entradas",
    lista.stream()
        .filter(m -> "ENTRADA".equals(m.tipo))
        .filter(m -> "PAGO".equals(m.statusPagamento))
        .map(m -> m.valor)
        .filter(java.util.Objects::nonNull)
        .reduce(BigDecimal.ZERO, (total, valor) -> total.add(valor))
);

model.addAttribute("saidas",
    lista.stream()
        .filter(m -> "SAIDA".equals(m.tipo))
        .filter(m -> "PAGO".equals(m.statusPagamento))
        .map(m -> m.valor)
        .filter(java.util.Objects::nonNull)
        .reduce(BigDecimal.ZERO, (total, valor) -> total.add(valor))
);}

   @GetMapping("/movimentacoes")
    public String inicio(Model model) {

        dados(model, List.of());

        model.addAttribute("mes", YearMonth.now());

        return "movimentacoes";
    }

    @GetMapping("/movimentacoes/historico")
    public String historico(@RequestParam(required=false)String periodo,Model model){
        YearMonth atual=YearMonth.now();YearMonth mes=atual;
        try{
            if(periodo!=null)mes=YearMonth.parse(periodo);
        }
        catch(Exception e){
            return "redirect:/movimentacoes/historico";
        }

        if(mes.isAfter(atual)||mes.isBefore(atual.minusMonths(3)))
            return "redirect:/relatorios";

        dados(model,service.mes(mes));
        model.addAttribute("mes",mes);
        model.addAttribute("periodos",List.of(atual,atual.minusMonths(1),atual.minusMonths(2),atual.minusMonths(3)));
        return "historico";
    }

    @PostMapping("/movimentacoes")
    public String salvar(@Valid @ModelAttribute MovimentacaoForm form,org.springframework.validation.BindingResult errors,RedirectAttributes flash){
        if(errors.hasErrors()){
            flash.addFlashAttribute("erro","Confira os campos obrigatórios.");
            return "redirect:/movimentacoes";
        }
        try{
            service.salvar(form);flash.addFlashAttribute("sucesso","Movimentação cadastrada.");
        }
            catch(Exception e){flash.addFlashAttribute("erro",e.getMessage());}return "redirect:/movimentacoes";
        }
}