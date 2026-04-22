package com.muhasebe.tablemap.repository;

import com.muhasebe.base.repository.BaseRepository;
import com.muhasebe.tablemap.domain.ViewMap;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class ViewMapRepository extends BaseRepository<ViewMap> {

    public ViewMapRepository() {
        super(ViewMap.class);
    }

    public Optional<ViewMap> findByViewName(String viewName) {
        return em.createQuery(
                "SELECT v FROM ViewMap v WHERE v.viewName = :n", ViewMap.class)
                .setParameter("n", viewName)
                .getResultList().stream().findFirst();
    }

    public int deleteByViewName(String viewName) {
        return em.createQuery("DELETE FROM ViewMap v WHERE v.viewName = :n")
                .setParameter("n", viewName)
                .executeUpdate();
    }
}
