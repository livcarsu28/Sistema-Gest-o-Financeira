package financeiro.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import financeiro.repository.UsuarioRepository;
import financeiro.repository.UsuarioRepository.DadosUsuario;

@Service
public class UsuarioService {

    private static final int LIMITE_MAXIMO_USUARIOS = 2;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<DadosUsuario> listarUsuarios() {
        return usuarioRepository.listarUsuarios();
    }

    @Transactional(readOnly = true)
    public int contarUsuariosAtivos() {
        return usuarioRepository.contarUsuariosAtivos();
    }

    @Transactional(readOnly = true)
    public boolean podeCadastrarUsuarioComum() {
        return usuarioRepository.contarUsuariosAtivos() < LIMITE_MAXIMO_USUARIOS;
    }

    @Transactional
    public DadosUsuario cadastrarUsuarioComum(String nomeCompleto, String email, String senha) {
        if (nomeCompleto == null || nomeCompleto.trim().isBlank()) {
            throw new IllegalArgumentException("O nome completo é obrigatório.");
        }
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        if (senha == null || senha.trim().length() < 6) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 6 caracteres.");
        }

        // Bloqueio transacional para evitar concorrência e race conditions
        int totalAtivos = usuarioRepository.contarUsuariosAtivosComBloqueio();
        if (totalAtivos >= LIMITE_MAXIMO_USUARIOS) {
            throw new IllegalStateException("O sistema já possui o limite de 2 usuários cadastrados (Administrador e Usuário Comum).");
        }

        // Garante que só pode haver exatamente 1 Usuário Comum
        int totalComuns = usuarioRepository.contarUsuariosPorNivel("USER");
        if (totalComuns > 0) {
            throw new IllegalStateException("Já existe um Usuário Comum cadastrado no sistema. O limite é de exatamente 1 Administrador e 1 Usuário Comum.");
        }

        String emailNormalizado = email.trim().toLowerCase();
        if (usuarioRepository.buscarPorEmail(emailNormalizado).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail.");
        }

        int idCargo = usuarioRepository.buscarIdCargoPorNivel("USER")
                .or(() -> usuarioRepository.buscarIdCargoPorNivel("USUARIO"))
                .orElseGet(() -> usuarioRepository.criarCargoSeNaoExistir("Usuário Comum", "Acesso operacional sem permissões administrativas", "USER"));

        String hash = passwordEncoder.encode(senha.trim());
        usuarioRepository.salvarUsuario(nomeCompleto.trim(), emailNormalizado, hash, idCargo);

        return usuarioRepository.listarUsuarios().stream()
                .filter(u -> u.email().equalsIgnoreCase(emailNormalizado))
                .findFirst()
                .orElse(new DadosUsuario(0, nomeCompleto.trim(), emailNormalizado, "Usuário Comum", "USER", true));
    }
}
