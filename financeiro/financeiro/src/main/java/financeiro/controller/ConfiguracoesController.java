package financeiro.controller;

import financeiro.repository.InstituicaoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracoes")
public class ConfiguracoesController {

    private final InstituicaoRepository instituicaoRepository;

    public ConfiguracoesController(InstituicaoRepository instituicaoRepository) {
        this.instituicaoRepository = instituicaoRepository;
    }

    @PostMapping("/endereco")
    public String atualizarEndereco(@RequestParam("logradouro") String logradouro,
            @RequestParam("numero") String numero,
            @RequestParam("bairro") String bairro,
            @RequestParam("cidade") String cidade,
            @RequestParam("uf") String uf,
            @RequestParam("cep") String cep,
            RedirectAttributes redirectAttributes) {

        instituicaoRepository.atualizarEndereco(1, logradouro, numero, bairro, cidade, uf, cep);

        redirectAttributes.addFlashAttribute("sucesso", "Endereço atualizado com sucesso!");
        return "redirect:/configuracoes";
    }
}
