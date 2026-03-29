package com.company.password_manager;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class PasswordService {

    private final PasswordRepository passwordRepository;
    private final EncryptionService encryptionService;
    private final SharedAccessRepository sharedAccessRepository;

    public PasswordService(PasswordRepository passwordRepository,
                           EncryptionService encryptionService,
                           SharedAccessRepository sharedAccessRepository) {
        this.passwordRepository = passwordRepository;
        this.encryptionService = encryptionService;
        this.sharedAccessRepository = sharedAccessRepository;
    }

    public List<Password> getPasswords(Long telegramId) {
        return passwordRepository.findByTelegramId(telegramId);
    }

    public Password getPasswordById(Long id) {
        return passwordRepository.findById(id).orElseThrow();
    }

    public Password addPassword(Long telegramId, String serviceName, String login, String password) throws Exception {
        Password p = new Password();
        p.setTelegramId(telegramId);
        p.setServiceName(serviceName);
        p.setLogin(login);
        p.setEncryptedPassword(encryptionService.encrypt(password));
        return passwordRepository.save(p);
    }

    public String getDecryptedPassword(Long id) throws Exception {
        Password p = passwordRepository.findById(id).orElseThrow();
        return encryptionService.decrypt(p.getEncryptedPassword());
    }

    public boolean deletePassword(Long id) {
        if (passwordRepository.existsById(id)) {
            passwordRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public void grantAccess(Long ownerTelegramId, Long viewerTelegramId) {
        if (!sharedAccessRepository.existsByOwnerTelegramIdAndViewerTelegramId(ownerTelegramId, viewerTelegramId)) {
            SharedAccess access = new SharedAccess();
            access.setOwnerTelegramId(ownerTelegramId);
            access.setViewerTelegramId(viewerTelegramId);
            sharedAccessRepository.save(access);
        }
    }

    public List<Password> getSharedPasswords(Long viewerTelegramId) {
        List<SharedAccess> accessList = sharedAccessRepository.findByViewerTelegramId(viewerTelegramId);
        List<Password> result = new ArrayList<>();
        for (SharedAccess access : accessList) {
            result.addAll(passwordRepository.findByTelegramId(access.getOwnerTelegramId()));
        }
        return result;
    }
}
