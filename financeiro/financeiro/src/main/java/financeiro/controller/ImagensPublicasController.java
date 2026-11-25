package financeiro.controller;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import financeiro.repository.ImagemRepository;
import financeiro.repository.ImagemRepository.ImagemPublica;

@RestController
@CrossOrigin(origins = {"https://resgatedevidascriancafeliz.ong.br", "https://www.resgatedevidascriancafeliz.ong.br"})
public class ImagensPublicasController {
    private final ImagemRepository imagens;

    public ImagensPublicasController(ImagemRepository imagens) {
        this.imagens = imagens;
    }

    @GetMapping("/api/site-publico/imagens")
    public ResponseEntity<List<ImagemPublica>> listar() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(imagens.listarPublicadas());
    }

    @GetMapping("/api/site-publico/imagens/{id}")
    public ResponseEntity<byte[]> arquivo(@PathVariable int id) {
        return imagens.carregarPublicada(id).map(imagem -> ResponseEntity.ok()
                .contentType(imagem.nome().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .body(imagem.conteudo())).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
