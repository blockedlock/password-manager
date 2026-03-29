package com.company.password_manager;

import jakarta.persistence.*;

@Entity
@Table(name = "passwords")
public class Password {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceName;
    private String login;
    private String encryptedPassword;
    private Long telegramId;

    public Long getId() { return id; }
    public String getServiceName() { return serviceName; }
    public String getLogin() { return login; }
    public String getEncryptedPassword() { return encryptedPassword; }
    public Long getTelegramId() { return telegramId; }

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public void setLogin(String login) { this.login = login; }
    public void setEncryptedPassword(String encryptedPassword) { this.encryptedPassword = encryptedPassword; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }
}
