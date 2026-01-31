package com.tathang.example304.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tathang.example304.model.PasswordResetToken;
import com.tathang.example304.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUserAndUsedFalse(User user);

    Optional<PasswordResetToken> findByOtpAndUsedFalse(String otp);

    void deleteByUser(User user);

    void deleteByExpiryDateBefore(LocalDateTime date);

    Optional<PasswordResetToken> findByUserAndOtpAndUsedFalse(User user, String otp);
}