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
import java.util.Optional;

/**
 * Kullanici authentication ve yonetim servisi.
 * Sifreler BCrypt ile hashlenir, asla duz metin saklanmaz.
 */
@Stateless
public class UserService {

    @Inject
    private UserRepository userRepository;

    /**
     * Login dogrulamasi.
     * @return basariliysa UserEntity, degilse null
     */
    public UserEntity authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank()) return null;
        if (rawPassword == null || rawPassword.isBlank()) return null;

        Optional<UserEntity> opt = userRepository.findByUsername(username.trim());

        if (opt.isEmpty()) {
            System.out.println(">>> DEBUG: Kullanıcı bulunamadı: " + username);
            return null;
        }

        UserEntity user = opt.get();
        boolean isPasswordMatch = BCrypt.checkpw(rawPassword, user.getPasswordHash());

        System.out.println(">>> DEBUG: Kullanıcı bulundu: " + user.getUsername());
        System.out.println(">>> DEBUG: Şifre eşleşme sonucu: " + isPasswordMatch);

        if (!isPasswordMatch) {
            return null;
        }

        // Login zamanini guncelle
        user.setLastLogin(LocalDateTime.now());
        userRepository.update(user);

        return user;
    }

    /**
     * Yeni kullanici olustur (sifre BCrypt ile hashlenir).
     */
    public UserEntity createUser(String username, String rawPassword, UserRole role, String fullName) {
        UserEntity u = new UserEntity();
        u.setUsername(username);
        u.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt(10)));
        u.setRole(role);
        u.setFullName(fullName);
        u.setCreatedDate(LocalDateTime.now());
        userRepository.save(u);
        return u;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }



}