package com.example.erp.config;

import com.example.erp.domain.User;
import com.example.erp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInit {

    @Bean
    CommandLineRunner init(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByUsername("admin").isEmpty()) {
                User u = new User();
                u.setEmpNo("E0001");
                u.setName("관리자");
                u.setDept("IT");
                u.setUsername("admin");
                u.setPassword(encoder.encode("1234"));
                u.setRole("ROLE_ADMIN");
                u.setActive(true);

                repo.save(u);
            }
        };
    }
}
