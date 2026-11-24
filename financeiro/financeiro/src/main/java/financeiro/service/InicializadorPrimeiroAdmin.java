package financeiro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import financeiro.repository.UsuarioRepository;

/**
 * Componente responsável pelo provisionamento seguro do primeiro administrador
 * da instituição quando o banco de dados ainda não possui nenhum usuário cadastrado.
 *
 * Utiliza variáveis de ambiente (ADMIN_EMAIL, ADMIN_SENHA e opcionalmente ADMIN_NOME).
 * Se as variáveis não estiverem definidas, não cria credenciais fictícias ou padrão,
 * emitindo um alerta em log.
 *
 * Após a criação do primeiro administrador, este mecanismo não executa mais.
 */
@Component
public class InicializadorPrimeiroAdmin {

    private static final Logger log = LoggerFactory.getLogger(InicializadorPrimeiroAdmin.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nomeAdmin;
    private final String emailAdmin;
    private final String senhaAdmin;

    public InicializadorPrimeiroAdmin(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.nome:${ADMIN_NOME:Administrador ONG}}") String nomeAdmin,
            @Value("${app.admin.email:${ADMIN_EMAIL:}}") String emailAdmin,
            @Value("${app.admin.senha:${ADMIN_SENHA:}}") String senhaAdmin) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nomeAdmin = nomeAdmin;
        this.emailAdmin = emailAdmin;
        this.senhaAdmin = senhaAdmin;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        inicializarPrimeiroAdministradorSeNecessario();
    }

    @Transactional
    public boolean inicializarPrimeiroAdministradorSeNecessario() {
        int totalAtivos = usuarioRepository.contarUsuariosAtivos();
        if (totalAtivos > 0) {
            log.debug("Sistema já possui {} usuário(s) cadastrado(s). Inicialização de administrador dispensada.", totalAtivos);
            return false;
        }

        if (emailAdmin == null || emailAdmin.trim().isBlank() || senhaAdmin == null || senhaAdmin.trim().isBlank()) {
            log.warn("Nenhum usuário cadastrado no sistema e as variáveis de ambiente ADMIN_EMAIL/ADMIN_SENHA não foram configuradas. "
                    + "Para provisionar o primeiro administrador com segurança, configure ADMIN_EMAIL e ADMIN_SENHA no ambiente.");
            return false;
        }

        String email = emailAdmin.trim().toLowerCase();
        String senha = senhaAdmin.trim();
        String nome = (nomeAdmin != null && !nomeAdmin.trim().isBlank()) ? nomeAdmin.trim() : "Administrador ONG";

        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            log.error("O e-mail configurado em ADMIN_EMAIL ({}) é inválido. O primeiro administrador não foi provisionado.", email);
            return false;
        }

        if (senha.length() < 6) {
            log.error("A senha configurada em ADMIN_SENHA possui menos de 6 caracteres. O primeiro administrador não foi provisionado.");
            return false;
        }

        int idCargo = usuarioRepository.buscarIdCargoPorNivel("ADMIN")
                .or(() -> usuarioRepository.buscarIdCargoPorNivel("ADMINISTRADOR"))
                .orElseGet(() -> usuarioRepository.criarCargoSeNaoExistir(
                        "Administrador", "Acesso administrativo completo ao sistema", "ADMIN"));

        String senhaHash = passwordEncoder.encode(senha);
        usuarioRepository.salvarUsuario(nome, email, senhaHash, idCargo);

        log.info("Primeiro administrador provisionado com sucesso para o e-mail [{}] com perfil ADMIN.", email);
        return true;
    }
}
