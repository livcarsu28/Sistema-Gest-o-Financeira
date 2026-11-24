package financeiro.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UsuarioAutenticado implements UserDetails {

    private final Integer id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final boolean status;
    private final LocalDateTime dataExclusao;
    private final String nivelPermissao;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioAutenticado(Integer id, String nome, String email, String senhaHash,
                              boolean status, LocalDateTime dataExclusao, String nivelPermissao) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.status = status;
        this.dataExclusao = dataExclusao;
        this.nivelPermissao = nivelPermissao;
        if (nivelPermissao == null || nivelPermissao.isBlank()) {
            this.authorities = Collections.emptyList();
        } else {
            String nivel = nivelPermissao.trim().toUpperCase();
            if ("USUARIO".equals(nivel) || "USER".equals(nivel)) {
                this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_USUARIO"));
            } else if ("ADMIN".equals(nivel) || "ADMINISTRADOR".equals(nivel)) {
                this.authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
            } else {
                this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + nivel));
            }
        }
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdUsuario() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getNomeCompleto() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public boolean isStatus() {
        return status;
    }

    public LocalDateTime getDataExclusao() {
        return dataExclusao;
    }

    public String getNivelPermissao() {
        return nivelPermissao;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return dataExclusao == null;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status && dataExclusao == null;
    }

    @Override
    public String toString() {
        return "UsuarioAutenticado[id=" + id + ", nome=" + nome + ", email=" + email + ", status=" + status
                + ", nivelPermissao=" + nivelPermissao + ", dataExclusao=" + dataExclusao + "]";
    }
}
