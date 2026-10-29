package com.hotelaria.config;

import com.hotelaria.models.Role;
import com.hotelaria.models.User;
import com.hotelaria.models.enums.RoleName;
import com.hotelaria.repositories.RoleRepository;
import com.hotelaria.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Cria o primeiro administrador na inicialização, somente quando:
 * <ul>
 *   <li>app.bootstrap.admin.enabled=true;</li>
 *   <li>ainda não existe nenhum administrador no banco;</li>
 *   <li>e-mail e senha (12+ caracteres) foram informados por variáveis de ambiente.</li>
 * </ul>
 * Depois do primeiro acesso, recomenda-se desabilitar o bootstrap e trocar a senha.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    static final int MIN_PASSWORD_LENGTH = 12;

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(AdminBootstrapProperties properties, UserRepository userRepository,
                                RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }
        if (userRepository.existsByRole_Name(RoleName.ROLE_ADMIN)) {
            log.info("Bootstrap de administrador ignorado: já existe um administrador cadastrado.");
            return;
        }
        if (!StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
            log.warn("Bootstrap de administrador habilitado, mas APP_BOOTSTRAP_ADMIN_EMAIL e "
                    + "APP_BOOTSTRAP_ADMIN_PASSWORD não foram definidos. Nenhum administrador foi criado.");
            return;
        }
        if (properties.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD deve ter no mínimo "
                    + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Já existe um usuário não administrador com o e-mail " + email);
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Papel ROLE_ADMIN não encontrado; verifique as migrations"));
        String name = StringUtils.hasText(properties.name()) ? properties.name().trim() : "Administrador";
        userRepository.save(new User(name, email, passwordEncoder.encode(properties.password()), adminRole));
        log.info("Administrador inicial criado com o e-mail {}. Desabilite o bootstrap após o primeiro acesso.", email);
    }
}
