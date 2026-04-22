package com.muhasebe.category.domain;

import com.muhasebe.base.domain.BaseAuditingEntity;
import jakarta.persistence.*;

/**
 * Category icin detay alan tanimlari (ornek: renk, boyut, marka).
 * Bir CategoryEntity birden fazla CategoryDetailsEntity'ye sahip olabilir (M2M).
 */
@Entity
@Table(name = "category_details")
public class CategoryDetailsEntity extends BaseAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "data_type", length = 50)
    private String dataType = "string";  // string, number, boolean, date

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
}
