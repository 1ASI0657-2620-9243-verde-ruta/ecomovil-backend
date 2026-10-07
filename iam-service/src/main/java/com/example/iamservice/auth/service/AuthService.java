package com.example.iamservice.auth.service;

import com.example.iamservice.auth.dto.LoginRequest;
import com.example.iamservice.auth.dto.RegisterRequest;
import com.example.iamservice.user.model.User;
import com.example.iamservice.user.model.UserRole;
import com.example.iamservice.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(RegisterRequest request) {

        if (userRepository.existsByEmail(
                request.getEmail())) {

            throw new IllegalStateException(
                    "El correo ya está registrado"
            );
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Por ahora simplificamos el proyecto.
        // Después podemos agregar BCrypt/JWT.
        user.setPassword(request.getPassword());

        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);

        return userRepository.save(user);
    }

    public User login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Correo o contraseña incorrectos"
                        )
                );

        if (!user.getPassword()
                .equals(request.getPassword())) {

            throw new IllegalStateException(
                    "Correo o contraseña incorrectos"
            );
        }

        if (!user.getActive()) {

            throw new IllegalStateException(
                    "El usuario está desactivado"
            );
        }

        return user;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}