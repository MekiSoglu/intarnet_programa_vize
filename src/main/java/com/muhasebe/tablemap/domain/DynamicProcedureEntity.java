package com.muhasebe.tablemap.domain;

import com.muhasebe.base.domain.BaseEntity;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Kullanicinin tanimladigi dinamik procedure'lerin metadatasi.
 * Asil procedure PostgreSQL icinde saklaniyor; bu tablo sadece UI'da listelemek icin.
 */
@Entity
@Table(name = "dynamic_procedures")
public class DynamicProcedureEntity implements BaseEntity, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "procedure_name", nullable = false, unique = true, length = 255)
    private String procedureName;

    @Column(name = "definition_json", columnDefinition = "TEXT")
    private String definitionJson;

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) createdDate = LocalDateTime.now();
    }

    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public String getProcedureName() { return procedureName; }
    public void setProcedureName(String procedureName) { this.procedureName = procedureName; }

    public String getDefinitionJson() { return definitionJson; }
    public void setDefinitionJson(String definitionJson) { this.definitionJson = definitionJson; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
