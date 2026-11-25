
package financeiro.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import financeiro.repository.ImagemRepository;
import financeiro.repository.ImagemRepository.DadosImagemMidia;

@Service
public class ImagemService {

    private static final Logger log = LoggerFactory.getLogger(ImagemService.class);

    public static final int LIMITE_MAXIMO = 10;
    public static final long TAMANHO_MAXIMO_BYTES = 10 * 1024 * 1024;

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of("png", "jpg", "jpeg");

    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final byte[] JPEG_MAGIC = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };

    private final ImagemRepository imagemRepository;
    private final Path uploadDir;

    // Construtor utilizado pelo Spring
    @Autowired
    public ImagemService(
            ImagemRepository imagemRepository,
            @Value("${app.upload.dir:${UPLOAD_DIR:uploads}}") String uploadDirProperty) {

        this.imagemRepository = imagemRepository;
        this.uploadDir = Paths.get(uploadDirProperty)
                .toAbsolutePath()
                .normalize();

        criarDiretorioUpload();
    }

    // Construtor auxiliar utilizado pelos testes
    public ImagemService(
            ImagemRepository imagemRepository,
            Path customUploadDir) {

        this.imagemRepository = imagemRepository;
        this.uploadDir = customUploadDir
                .toAbsolutePath()
                .normalize();

        criarDiretorioUpload();
    }

    private void criarDiretorioUpload() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            log.error(
                    "Não foi possível criar o diretório de uploads: {}",
                    uploadDir,
                    e);
            throw new IllegalStateException(
                    "Falha ao inicializar o diretório de uploads.",
                    e);
        }
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    @Transactional(readOnly = true)
    public int contarImagens() {
        return imagemRepository.contarTotal();
    }

    @Transactional(readOnly = true)
    public boolean podeAdicionar() {
        return contarImagens() < LIMITE_MAXIMO;
    }

    @Transactional(readOnly = true)
    public List<DadosImagemMidia> listarImagens() {
        return imagemRepository.listarTodas();
    }

    @Transactional
    public DadosImagemMidia salvarArquivoEAdicionar(
            String categoria,
            String nomeOriginal,
            byte[] bytes,
            Integer idUsuario,
            boolean publicar) {

        if (imagemRepository.contarTotal() >= LIMITE_MAXIMO) {
            throw new IllegalStateException(
                    "Limite de 10 imagens atingido. Remova uma imagem para adicionar outra.");
        }

        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do arquivo da imagem é obrigatório.");
        }

        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException(
                    "Nenhum conteúdo enviado para a imagem.");
        }

        if (bytes.length > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException(
                    "O arquivo excede o limite permitido de 10 MB.");
        }

        int ponto = nomeOriginal.lastIndexOf('.');

        if (ponto <= 0 || ponto == nomeOriginal.length() - 1) {
            throw new IllegalArgumentException(
                    "Formato não suportado. Envie uma imagem PNG ou JPG.");
        }

        String extensao = nomeOriginal
                .substring(ponto + 1)
                .toLowerCase();

        if (!EXTENSOES_PERMITIDAS.contains(extensao)) {
            throw new IllegalArgumentException(
                    "Formato não suportado. Envie uma imagem PNG ou JPG.");
        }

        validarMagicBytes(bytes, extensao);

        String nomeArquivoSalvo = UUID.randomUUID() + "." + extensao;

        String cat = categoria == null || categoria.isBlank() ? "Projetos" : categoria.trim();
        if (cat.length() > 50) throw new IllegalArgumentException("Categoria inválida.");

        String urlAcesso = "/configuracoes/imagens/arquivo/" + nomeArquivoSalvo;
        imagemRepository.salvar(cat, urlAcesso, idUsuario != null ? idUsuario : 1, bytes, publicar);
        return imagemRepository.listarTodas().stream()
                .filter(imagem -> imagem.urlImagem().equals(urlAcesso))
                .findFirst().orElseThrow();
    }

    @Transactional
    public void removerImagem(int idMidia) {

        var opt = imagemRepository.buscarPorId(idMidia);

        if (opt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Imagem não encontrada para remoção.");
        }

        DadosImagemMidia midia = opt.get();

        imagemRepository.excluir(idMidia);

        try {
            String url = midia.urlImagem();

            if (url != null && url.contains("/")) {
                String nomeArquivo = url
                        .substring(url.lastIndexOf('/') + 1)
                        .trim();

                if (!nomeArquivo.contains("..") && !nomeArquivo.isBlank()) {
                    Path arquivoFisico = uploadDir
                            .resolve(nomeArquivo)
                            .normalize();

                    if (arquivoFisico.startsWith(uploadDir)) {
                        Files.deleteIfExists(arquivoFisico);
                    }
                }
            }

        } catch (Exception e) {
            log.warn(
                    "Não foi possível excluir o arquivo físico da imagem id {}: {}",
                    idMidia,
                    e.getMessage());
        }
    }

    public Resource carregarArquivoComoRecurso(String nomeArquivo) {

        if (nomeArquivo == null
                || nomeArquivo.contains("..")
                || nomeArquivo.contains("/")
                || nomeArquivo.contains("\\")) {

            throw new IllegalArgumentException(
                    "Nome de arquivo inválido.");
        }

        var conteudo = imagemRepository.carregarConteudo("/configuracoes/imagens/arquivo/" + nomeArquivo);
        if (conteudo.isPresent()) {
            return new ByteArrayResource(conteudo.get()) {
                @Override
                public String getFilename() {
                    return nomeArquivo;
                }
            };
        }

        Path arquivo = uploadDir
                .resolve(nomeArquivo)
                .normalize();

        if (!arquivo.startsWith(uploadDir)
                || !Files.exists(arquivo)
                || !Files.isReadable(arquivo)) {

            throw new IllegalArgumentException(
                    "Arquivo de imagem não encontrado ou indisponível.");
        }

        try {
            Resource recurso = new UrlResource(arquivo.toUri());

            if (!recurso.exists() || !recurso.isReadable()) {
                throw new IllegalArgumentException(
                        "Não foi possível ler o arquivo de imagem.");
            }

            return recurso;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Erro ao carregar o arquivo de imagem.",
                    e);
        }
    }

    private void validarMagicBytes(byte[] bytes, String extensao) {

        if ("png".equals(extensao)) {

            if (bytes.length < PNG_MAGIC.length) {
                throw new IllegalArgumentException(
                        "Conteúdo do arquivo não corresponde a uma imagem PNG válida.");
            }

            byte[] cabecalho = Arrays.copyOfRange(
                    bytes,
                    0,
                    PNG_MAGIC.length);

            if (!Arrays.equals(cabecalho, PNG_MAGIC)) {
                throw new IllegalArgumentException(
                        "Conteúdo do arquivo não corresponde a uma imagem PNG válida.");
            }

        } else if ("jpg".equals(extensao) || "jpeg".equals(extensao)) {

            if (bytes.length < JPEG_MAGIC.length) {
                throw new IllegalArgumentException(
                        "Conteúdo do arquivo não corresponde a uma imagem JPG válida.");
            }

            byte[] cabecalho = Arrays.copyOfRange(
                    bytes,
                    0,
                    JPEG_MAGIC.length);

            if (!Arrays.equals(cabecalho, JPEG_MAGIC)) {
                throw new IllegalArgumentException(
                        "Conteúdo do arquivo não corresponde a uma imagem JPG válida.");
            }
        }

        try {
            var imagemLida = ImageIO.read(
                    new ByteArrayInputStream(bytes));

            if (imagemLida == null) {
                throw new IllegalArgumentException(
                        "Arquivo não pôde ser decodificado como uma imagem válida.");
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Erro ao processar conteúdo gráfico da imagem.",
                    e);
        }
    }
}
