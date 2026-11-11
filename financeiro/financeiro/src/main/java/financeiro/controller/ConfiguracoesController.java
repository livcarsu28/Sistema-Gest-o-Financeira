package financeiro.controller;

import financeiro.repository.InstituicaoRepository;
import financeiro.repository.InstituicaoRepository.Instituicao;
import financeiro.service.PixService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/configuracoes")
public class ConfiguracoesController {

    private final InstituicaoRepository instituicaoRepository;
    private final PixService pixService;

    // Injetando tanto o repositório quanto o PixService
    public ConfiguracoesController(InstituicaoRepository instituicaoRepository, PixService pixService) {
        this.instituicaoRepository = instituicaoRepository;
        this.pixService = pixService;
    }

    @GetMapping
    public String exibirConfiguracoes(Model model) {
        // Usa o método da equipe sem bloquear o banco para leitura na tela
        List<Instituicao> lista = instituicaoRepository.consultar(false);
        Instituicao instituicao;

        if (lista.isEmpty()) {
            // Se o banco estiver vazio, manda um objeto zerado para a tela não quebrar
            instituicao = new Instituicao(1, "Não cadastrada", "Rua não informada", "S/N", "Bairro", "Cidade", "UF",
                    "00000-000");
        } else {
            instituicao = lista.get(0); // Pega o ID 1
        }

        model.addAttribute("instituicao", instituicao);
        return "configuracoes";
    }

    @PostMapping("/pix")
    public String atualizarChavePix(@RequestParam("chavePix") String chavePix, RedirectAttributes redirectAttributes) {
        try {
            // Agora usamos o PixService para manter as validações de negócio da equipe
            pixService.salvar(chavePix);
            redirectAttributes.addFlashAttribute("sucesso", "Chave PIX atualizada com sucesso!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Captura erros de validação/estado e envia para a tela de configurações
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        } catch (DataAccessException e) {
            // Captura erro de banco fora do ar
            redirectAttributes.addFlashAttribute("erro", "Não foi possível salvar a chave Pix. Tente novamente.");
        }

        return "redirect:/configuracoes";
    }

    @PostMapping("/endereco")
    public String atualizarEndereco(@RequestParam("logradouro") String logradouro,
            @RequestParam("numero") String numero,
            @RequestParam("bairro") String bairro,
            @RequestParam("cidade") String cidade,
            @RequestParam("uf") String uf,
            @RequestParam("cep") String cep,
            RedirectAttributes redirectAttributes) {

        // Atualiza o endereço do ID 1 usando o método JDBC direto
        instituicaoRepository.atualizarEndereco(1, logradouro, numero, bairro, cidade, uf, cep);

        redirectAttributes.addFlashAttribute("sucesso", "Endereço atualizado com sucesso!");
        return "redirect:/configuracoes";
    }
}