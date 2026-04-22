package com.muhasebe.category.service;

import com.muhasebe.base.service.BaseService;
import com.muhasebe.category.domain.CategoryDetailsEntity;
import com.muhasebe.category.repository.CategoryDetailsRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class CategoryDetailsService extends BaseService<CategoryDetailsEntity, CategoryDetailsRepository> {

    @Inject
    private CategoryDetailsRepository repository;

    @Override
    protected CategoryDetailsRepository getRepository() {
        return repository;
    }
}
