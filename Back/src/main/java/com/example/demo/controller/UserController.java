package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.InputSanitizer;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;


@RestController
@RequestMapping("/users")
@CrossOrigin
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {
        // Sanitize inputs
        user.setEmail(InputSanitizer.sanitizeEmail(user.getEmail()));
        user.setName(InputSanitizer.sanitize(user.getName()));

        // Validate after sanitization
        if (user.getEmail() == null || user.getEmail().isBlank() ||
            user.getPassword() == null || user.getPassword().isBlank() ||
            user.getName() == null || user.getName().isBlank()) {
            return ResponseEntity.badRequest().body("MISSING_FIELDS");
        }

        // Check password length
        if (user.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body("PASSWORD_TOO_SHORT");
        }

        // Check existing email
        if (userRepo.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.status(409).body("EMAIL_ALREADY_EXISTS");
        }

        try {
            // Hash password before saving
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            User saved = userRepo.save(user);

            // Don't return password in response
            saved.setPassword(null);

            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).body("EMAIL_ALREADY_EXISTS");
        } catch (Exception ex) {
            System.err.println("Registration error: " + ex.getMessage());
            return ResponseEntity.status(500).body("REGISTRATION_FAILED");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User login, HttpServletRequest request, HttpServletResponse response) {
        // Sanitize email
        String email = InputSanitizer.sanitizeEmail(login.getEmail());

        if (email == null || email.isBlank() || login.getPassword() == null) {
            return ResponseEntity.badRequest().body("MISSING_CREDENTIALS");
        }

        User u = userRepo.findByEmail(email);

        if (u == null) {
            return ResponseEntity.status(401).body("INVALID_CREDENTIALS");
        }

        // Use BCrypt to compare passwords
        if (!passwordEncoder.matches(login.getPassword(), u.getPassword())) {
            return ResponseEntity.status(401).body("INVALID_CREDENTIALS");
        }

        // Create HTTP session
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", u.getId());
        session.setAttribute("username", u.getName());

        // Build Set-Cookie header
        ResponseCookie cookie = ResponseCookie.from("JSESSIONID", session.getId())
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Don't return password in response
        u.setPassword(null);

        return ResponseEntity.ok(u);
    }

    @GetMapping("/all")
    public List<User> getAll() {
        List<User> users = userRepo.findAll();
        // Remove passwords from response
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        return userRepo.findById(id)
                .map(user -> {
                    user.setPassword(null);
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.status(404).body(null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody User updated) {
        return userRepo.findById(id)
                .map(user -> {
                    if (updated.getName() != null && !updated.getName().isBlank()) {
                        user.setName(InputSanitizer.sanitize(updated.getName()));
                    }
                    if (updated.getEmail() != null && !updated.getEmail().isBlank()) {
                        String sanitizedEmail = InputSanitizer.sanitizeEmail(updated.getEmail());
                        // Check if email is already taken by another user
                        User existing = userRepo.findByEmail(sanitizedEmail);
                        if (existing != null && !existing.getId().equals(user.getId())) {
                            return ResponseEntity.status(409).body("EMAIL_ALREADY_EXISTS");
                        }
                        user.setEmail(sanitizedEmail);
                    }
                    User saved = userRepo.save(user);
                    saved.setPassword(null);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.status(404).body(null));
    }

}
