package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_EMAIL = "admin";
    private static final String ADMIN_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User existingAdmin = userRepository.findByEmail(ADMIN_EMAIL);
        if (existingAdmin != null) {
            System.out.println("Admin user already exists with id: " + existingAdmin.getId());
            return;
        }

        User admin = new User();
        admin.setName(ADMIN_NAME);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));

        try {
            User saved = userRepository.save(admin);
            System.out.println("Created admin user with id: " + saved.getId());
        } catch (Exception ex) {
            System.err.println("Failed to create admin user: " + ex.getMessage());
        }
    }
}
