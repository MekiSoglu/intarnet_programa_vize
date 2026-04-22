package com.muhasebe.tablemap.service;

import com.muhasebe.base.service.BaseService;
import com.muhasebe.tablemap.domain.SystemLogEntity;
import com.muhasebe.tablemap.repository.SystemLogRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;

@Stateless
public class SystemLogService extends BaseService<SystemLogEntity, SystemLogRepository> {

    @Inject
    private SystemLogRepository repository;

    @Override
    protected SystemLogRepository getRepository() {
        return repository;
    }

    public List<SystemLogEntity> getRecentLogs() {
        return repository.findAllOrderByTarihDesc();
    }
}