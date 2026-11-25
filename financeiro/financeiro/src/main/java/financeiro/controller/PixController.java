package financeiro.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import financeiro.service.PixService;

@RestController
public class PixController {

    private final PixService pixService;

    public PixController(PixService pixService) {
        this.pixService = pixService;
    }

    @PostMapping("/configuracoes/pix")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> salvar(@RequestParam String chavePix) {
        try {
            return ResponseEntity.ok(pixService.salvar(chavePix));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Erro("Apenas administradores podem alterar a chave PIX."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Erro(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new Erro(e.getMessage()));
        } catch (DataAccessException e) {
            return ResponseEntity.status(503).body(new Erro("Não foi possível salvar a chave Pix. Tente novamente."));
        }
    }

    @PostMapping("/configuracoes/pix/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView alterarStatus(@RequestParam boolean ativo, RedirectAttributes redirect) {
        try {
            pixService.alterarStatus(ativo);
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("erroStatusPix", e.getMessage());
        } catch (DataAccessException e) {
            redirect.addFlashAttribute("erroStatusPix", "Não foi possível alterar o Pix. Tente novamente.");
        }
        return new ModelAndView("redirect:/configuracoes");
    }

    public record Erro(String mensagem) {
    }
}
