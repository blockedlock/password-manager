package com.company.password_manager;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SharedAccessRepository extends JpaRepository<SharedAccess, Long> {
    List<SharedAccess> findByOwnerTelegramId(Long ownerTelegramId);
    List<SharedAccess> findByViewerTelegramId(Long viewerTelegramId);
    boolean existsByOwnerTelegramIdAndViewerTelegramId(Long ownerTelegramId, Long viewerTelegramId);
}
