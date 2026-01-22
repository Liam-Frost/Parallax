package com.parallax.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    Optional<UserEntity> findByPhoneCountryAndPhoneDigits(String phoneCountry, String phoneDigits);

    @Query("select u from UserEntity u where u.phoneDigits = :digits")
    Optional<UserEntity> findByPhoneDigits(@Param("digits") String digits);
}
