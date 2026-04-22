package com.muhasebe.base.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Audit alanlari iceren ortak super-class.
 * Entity'ler @MappedSuperclass yardimiyla bu sinifi extend eder ve audit kolonlarini kazanir.
 *
 * NOT: Spring'deki @CreatedBy/@CreatedDate annotation'larinin Jakarta karsiligi olmadigi icin
 *      audit alanlarini @PrePersist / @PreUpdate ile kendimiz dolduruyoruz.
 */
@MappedSuperclass
public abstract class BaseAuditingEntity implements BaseEntity {

    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Version
    @Column(name = "version")
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (createdBy == null) {
            createdBy = "system";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
        if (lastModifiedBy == null) {
            lastModifiedBy = "system";
        }
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }

    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
