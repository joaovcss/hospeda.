package com.hotelaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// UserDetailsServiceAutoConfiguration é excluída para que o Spring Boot não crie
// um usuário em memória com senha gerada: toda autenticação passa pelo JWT.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class HotelariaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelariaApplication.class, args);
    }
}
