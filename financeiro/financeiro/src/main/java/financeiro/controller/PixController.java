package financeiro.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @DeleteMapping("/configuracoes/pix")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> desativar() {
        try {
            return ResponseEntity.ok(pixService.desativar());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Erro("Apenas administradores podem desativar a chave PIX."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new Erro(e.getMessage()));
        } catch (DataAccessException e) {
            return ResponseEntity.status(503).body(new Erro("Não foi possível desativar a chave Pix. Tente novamente."));
        }
    }

    public record Erro(String mensagem) {
    }
}
