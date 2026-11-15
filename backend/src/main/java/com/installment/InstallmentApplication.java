package com.installment;

import com.installment.entity.User;
import com.installment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class InstallmentApplication {

    private static final Logger log = LoggerFactory.getLogger(InstallmentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(InstallmentApplication.class, args);
    }

    @Bean
    public ApplicationRunner seedAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword,
            @Value("${app.admin.name}") String adminName) {
        return args -> {
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setName(adminName);
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("admin");
                userRepository.save(admin);
                log.info("Admin user seeded: {}", adminEmail);
            } else {
                log.info("Admin user already exists: {}", adminEmail);
            }
        };
    }
}

