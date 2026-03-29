package com.company.password_manager;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PasswordService {

    private final PasswordRepository passwordRepository;
    private final EncryptionService encryptionService;

    public PasswordService(PasswordRepository passwordRepository, EncryptionService encryptionService) {
        this.passwordRepository = passwordRepository;
        this.encryptionService = encryptionService;
    }

    public List<Password> getPasswords(Long telegramId) {
        return passwordRepository.findByTelegramId(telegramId);
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
}
