package com.example.bankcards.service;

import com.example.bankcards.dto.UserCreateRequest;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.UserMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(UserCreateRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BadRequestException("username already exists");
        }

        Role role = parseRole(req.getRole());

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setRole(role);
        user.setEnabled(true);

        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String q, Pageable pageable) {
        Page<User> page;
        if (q == null || q.isBlank()) {
            page = userRepository.findAll(pageable);
        } else {
            page = userRepository.search(q.trim(), pageable);
        }
        return page.map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public UserResponse updateRole(Long id, String roleRaw) {
        Role role = parseRole(roleRaw);
        User user = getEntity(id);
        user.setRole(role);
        return UserMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    private Role parseRole(String raw) {
        if (raw == null) {
            throw new BadRequestException("role is required");
        }
        try {
            return Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported role: " + raw);
        }
    }
}
