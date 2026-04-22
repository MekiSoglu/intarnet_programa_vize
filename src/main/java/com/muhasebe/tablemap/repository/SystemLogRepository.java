package com.muhasebe.tablemap.repository;

import com.muhasebe.base.repository.BaseRepository;
import com.muhasebe.tablemap.domain.SystemLogEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SystemLogRepository extends BaseRepository<SystemLogEntity> {

    public SystemLogRepository() {
        super(SystemLogEntity.class);
    }

    // En yeni işlemlerin en üstte gelmesi için özel sorgu
    public List<SystemLogEntity> findAllOrderByTarihDesc() {
        return em.createQuery(
                         "SELECT s FROM SystemLogEntity s ORDER BY s.tarih DESC",
                         SystemLogEntity.class)
                 .getResultList();
    }
}