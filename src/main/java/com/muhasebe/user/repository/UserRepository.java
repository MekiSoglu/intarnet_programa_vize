package com.muhasebe.user.repository;

import com.muhasebe.base.repository.BaseRepository;
import com.muhasebe.user.domain.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class UserRepository extends BaseRepository<UserEntity> {

    public UserRepository() {
        super(UserEntity.class);
    }

    public Optional<UserEntity> findByUsername(String username) {
        return em.createQuery(
                         "SELECT u FROM UserEntity u WHERE u.username = :u",
                         UserEntity.class)
                 .setParameter("u", username)
                 .getResultList()
                 .stream()
                 .findFirst();
    }
}