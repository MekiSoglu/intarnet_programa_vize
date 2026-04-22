package com.muhasebe.tablemap.repository;

import com.muhasebe.base.repository.BaseRepository;
import com.muhasebe.tablemap.domain.DynamicProcedureEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class DynamicProcedureRepository extends BaseRepository<DynamicProcedureEntity> {

    public DynamicProcedureRepository() {
        super(DynamicProcedureEntity.class);
    }

    public Optional<DynamicProcedureEntity> findByName(String name) {
        return em.createQuery(
                "SELECT p FROM DynamicProcedureEntity p WHERE p.procedureName = :n",
                DynamicProcedureEntity.class)
                .setParameter("n", name)
                .getResultList().stream().findFirst();
    }
}
