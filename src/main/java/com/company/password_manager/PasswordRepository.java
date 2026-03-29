package com.company.password_manager;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PasswordRepository extends JpaRepository<Password, Long> {
    List<Password> findByTelegramId(Long telegramId);
}
