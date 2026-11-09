
package financeiro.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FinanceiroController {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("paginaAtiva", "dashboard");
        return "dashboard";
    }

    @GetMapping("/calendario")
    public String calendario(Model model) {
        model.addAttribute("paginaAtiva", "calendario");
        return "calendario";
    }

    @GetMapping("/relatorios")
    public String relatorios(Model model) {
        model.addAttribute("paginaAtiva", "relatorios");
        return "relatorios";
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        model.addAttribute("paginaAtiva", "configuracoes");
        return "configuracoes";
    }
}
