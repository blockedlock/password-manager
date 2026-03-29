package com.company.password_manager;

import jakarta.persistence.*;

@Entity
@Table(name = "shared_access")
public class SharedAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerTelegramId;
    private Long viewerTelegramId;

    public Long getId() { return id; }
    public Long getOwnerTelegramId() { return ownerTelegramId; }
    public Long getViewerTelegramId() { return viewerTelegramId; }
    public void setOwnerTelegramId(Long ownerTelegramId) { this.ownerTelegramId = ownerTelegramId; }
    public void setViewerTelegramId(Long viewerTelegramId) { this.viewerTelegramId = viewerTelegramId; }
}
