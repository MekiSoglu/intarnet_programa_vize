package com.muhasebe.user.service;

import com.muhasebe.user.domain.UserEntity;
import com.muhasebe.user.domain.UserRole;
import com.muhasebe.user.repository.UserRepository;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Stateless
public class UserService {

    @Inject
    private UserRepository userRepository;

    public UserEntity authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank()) return null;
        if (rawPassword == null || rawPassword.isBlank()) return null;

        Optional<UserEntity> opt = userRepository.findByUsername(username.trim());
        if (opt.isEmpty()) return null;

        UserEntity user = opt.get();
        if (!BCrypt.checkpw(rawPassword, user.getPasswordHash())) {
            return null;
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.update(user);

        return user;
    }

    public UserEntity createUser(String username, String rawPassword, UserRole role, String fullName) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Kullanici adi bos olamaz");
        }
        if (rawPassword == null || rawPassword.length() < 4) {
            throw new IllegalArgumentException("Sifre en az 4 karakter olmali");
        }
        if (role == null) {
            throw new IllegalArgumentException("Rol secilmeli");
        }
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new IllegalArgumentException("Bu kullanici adi zaten kullanimda: " + username);
        }

        UserEntity u = new UserEntity();
        u.setUsername(username.trim());
        u.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt(10)));
        u.setRole(role);
        u.setFullName(fullName != null ? fullName.trim() : null);
        u.setCreatedDate(LocalDateTime.now());
        userRepository.save(u);
        return u;
    }

    /** Sifre degistir. */
    public void updatePassword(Long userId, String newRawPassword) {
        if (newRawPassword == null || newRawPassword.length() < 4) {
            throw new IllegalArgumentException("Sifre en az 4 karakter olmali");
        }
        UserEntity user = userRepository.findById(userId)
                                        .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));
        user.setPasswordHash(BCrypt.hashpw(newRawPassword, BCrypt.gensalt(10)));
        userRepository.update(user);
    }

    /** Rol degistir. */
    public void updateRole(Long userId, UserRole newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Rol bos olamaz");
        }
        UserEntity user = userRepository.findById(userId)
                                        .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));
        user.setRole(newRole);
        userRepository.update(user);
    }

    /** Kullanici sil. */
    public void deleteUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                                        .orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi"));
        userRepository.delete(user);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<UserEntity> listAll() {
        return userRepository.findAllOrderByUsername();
    }
}