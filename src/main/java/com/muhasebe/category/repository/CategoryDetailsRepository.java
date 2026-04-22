package com.muhasebe.category.repository;

import com.muhasebe.base.repository.BaseRepository;
import com.muhasebe.category.domain.CategoryDetailsEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoryDetailsRepository extends BaseRepository<CategoryDetailsEntity> {
    public CategoryDetailsRepository() {
        super(CategoryDetailsEntity.class);
    }
}
