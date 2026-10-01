
package financeiro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FinanceiroController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
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
    public String configuracoes() {
        return "configuracoes";
    }
}