package financeiro.config;

import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Configuração de UserDetailsService utilizada exclusivamente em testes @WebMvcTest.
 *
 * Resgata o usuário padrão de SecurityProperties (gerado pelo Spring Boot para ambiente
 * de desenvolvimento/teste), encodifica a senha com o PasswordEncoder real (BCrypt)
 * e provisiona o usuário em um InMemoryUserDetailsManager.
 *
 * Em produção, o AutenticacaoService (UserDetailsService real) é usado no lugar desta classe.
 * Esta configuração só é ativada quando importada explicitamente em testes.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(SecurityProperties securityProperties,
                                                 PasswordEncoder passwordEncoder) {
        var user = securityProperties.getUser();
        var userDetails = User.builder()
                .username(user.getName())
                .password(passwordEncoder.encode(user.getPassword()))
                .roles(user.getRoles().isEmpty() ? new String[]{"USER"} : user.getRoles().toArray(new String[0]))
                .build();
        return new InMemoryUserDetailsManager(userDetails);
    }
}
