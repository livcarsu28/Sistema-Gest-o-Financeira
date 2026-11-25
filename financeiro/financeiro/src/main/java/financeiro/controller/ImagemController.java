package financeiro.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import financeiro.repository.ImagemRepository.DadosImagemMidia;
import financeiro.security.UsuarioAutenticado;
import financeiro.service.ImagemService;

@RestController
@RequestMapping("/configuracoes/imagens")
public class ImagemController {

    private final ImagemService imagemService;

    public ImagemController(ImagemService imagemService) {
        this.imagemService = imagemService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listar() {
        List<DadosImagemMidia> imagens = imagemService.listarImagens();
        return ResponseEntity.ok(Map.of(
                "total", imagens.size(),
                "maximo", ImagemService.LIMITE_MAXIMO,
                "limiteAtingido", imagens.size() >= ImagemService.LIMITE_MAXIMO,
                "imagens", imagens));
    }

    @GetMapping("/arquivo/{nomeArquivo}")
    public ResponseEntity<Resource> obterArquivo(@PathVariable("nomeArquivo") String nomeArquivo) {
        try {
            Resource recurso = imagemService.carregarArquivoComoRecurso(nomeArquivo);
            MediaType mediaType = MediaTypeFactory.getMediaType(recurso)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(recurso);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> adicionar(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "categoria", defaultValue = "Projetos") String categoria,
            @RequestParam(value = "publicar", defaultValue = "false") boolean publicar,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        try {
            if (arquivo == null || arquivo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("mensagem", "Nenhum arquivo enviado."));
            }

            Integer idUsuario = usuario != null ? usuario.getId() : null;
            byte[] bytes = arquivo.getBytes();

            DadosImagemMidia criada = imagemService.salvarArquivoEAdicionar(
                    categoria,
                    arquivo.getOriginalFilename(),
                    bytes,
                    idUsuario,
                    publicar);

            int novoTotal = imagemService.contarImagens();
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensagem", "Imagem adicionada com sucesso!",
                    "imagem", criada,
                    "total", novoTotal,
                    "maximo", ImagemService.LIMITE_MAXIMO,
                    "limiteAtingido", novoTotal >= ImagemService.LIMITE_MAXIMO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "mensagem", e.getMessage(),
                    "total", imagemService.contarImagens(),
                    "maximo", ImagemService.LIMITE_MAXIMO,
                    "limiteAtingido", true));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensagem", "Erro ao processar o arquivo enviado."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remover(@PathVariable("id") int id) {
        try {
            imagemService.removerImagem(id);
            int novoTotal = imagemService.contarImagens();
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Imagem removida com sucesso!",
                    "total", novoTotal,
                    "maximo", ImagemService.LIMITE_MAXIMO,
                    "limiteAtingido", novoTotal >= ImagemService.LIMITE_MAXIMO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
        }
    }
}
