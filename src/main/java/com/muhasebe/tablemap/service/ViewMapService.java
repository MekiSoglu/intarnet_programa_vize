package com.muhasebe.tablemap.service;

import com.muhasebe.base.service.BaseService;
import com.muhasebe.tablemap.domain.ViewMap;
import com.muhasebe.tablemap.repository.ViewMapRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.Optional;

@Stateless
public class ViewMapService extends BaseService<ViewMap, ViewMapRepository> {

    @Inject
    private ViewMapRepository repository;

    @Override
    protected ViewMapRepository getRepository() {
        return repository;
    }

    public Optional<ViewMap> findByName(String viewName) {
        return repository.findByViewName(viewName);
    }

    public int deleteByName(String viewName) {
        return repository.deleteByViewName(viewName);
    }
}
