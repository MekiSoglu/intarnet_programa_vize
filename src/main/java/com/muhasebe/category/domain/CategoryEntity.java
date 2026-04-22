package com.muhasebe.category.domain;

import com.muhasebe.base.domain.BaseAuditingEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
public class CategoryEntity extends BaseAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /** Parent category - hiyerarsik yapi icin (Angular'daki categoryName parent-child). */
    @Column(name = "parent_id")
    private Long parentId;

    /** Bu kategoriye atanmis detay alanlari (many-to-many). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "category_detail_map",
        joinColumns = @JoinColumn(name = "category_id"),
        inverseJoinColumns = @JoinColumn(name = "detail_id")
    )
    private List<CategoryDetailsEntity> details = new ArrayList<>();

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public List<CategoryDetailsEntity> getDetails() { return details; }
    public void setDetails(List<CategoryDetailsEntity> details) { this.details = details; }
}
