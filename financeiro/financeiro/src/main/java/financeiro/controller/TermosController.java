package financeiro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TermosController {

    @GetMapping("/termos")
    public String termos(Model model) {
        model.addAttribute("abaAtiva", "termos");
        return "termos-e-politicas";
    }

    @GetMapping("/privacidade")
    public String privacidade(Model model) {
        model.addAttribute("abaAtiva", "privacidade");
        return "termos-e-politicas";
    }

    @GetMapping("/termos-e-politicas")
    public String termosEPoliticas(Model model) {
        model.addAttribute("abaAtiva", "todos");
        return "termos-e-politicas";
    }
}
