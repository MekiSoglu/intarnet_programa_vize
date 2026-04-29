package com.muhasebe.tablemap.domain;

import com.muhasebe.base.domain.BaseEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "sistem_loglari")
//aktif değil
public class SystemLogEntity implements BaseEntity, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "islem_adi", length = 255)
    private String islemAdi;

    @Column(name = "detay", columnDefinition = "TEXT")
    private String detay;

    @Column(name = "tarih", updatable = false)
    private LocalDateTime tarih;

    @PrePersist
    protected void onCreate() {
        if (tarih == null) {
            tarih = LocalDateTime.now();
        }
    }

    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public String getIslemAdi() { return islemAdi; }
    public void setIslemAdi(String islemAdi) { this.islemAdi = islemAdi; }

    public String getDetay() { return detay; }
    public void setDetay(String detay) { this.detay = detay; }

    public LocalDateTime getTarih() { return tarih; }
    public void setTarih(LocalDateTime tarih) { this.tarih = tarih; }
}