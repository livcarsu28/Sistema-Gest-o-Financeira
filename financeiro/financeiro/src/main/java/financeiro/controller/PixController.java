package financeiro.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> salvar(@RequestParam String chavePix) {
        try {
            return ResponseEntity.ok(pixService.salvar(chavePix));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Erro(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new Erro(e.getMessage()));
        } catch (DataAccessException e) {
            return ResponseEntity.status(503).body(new Erro("Não foi possível salvar a chave Pix. Tente novamente."));
        }
    }

    public record Erro(String mensagem) {
    }
}
