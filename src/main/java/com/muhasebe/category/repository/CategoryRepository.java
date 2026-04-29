package com.muhasebe.category.repository;

import com.muhasebe.base.repository.BaseRepository;
import com.muhasebe.category.domain.CategoryEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CategoryRepository extends BaseRepository<CategoryEntity> {
    public CategoryRepository() {
        super(CategoryEntity.class);
    }

    //base categoriyi çağırır
    public List<CategoryEntity> findRoots() {
        return em.createQuery(
                "SELECT c FROM CategoryEntity c WHERE c.parentId IS NULL ORDER BY c.name",
                CategoryEntity.class)
                .getResultList();
    }

    // üst kategorinini alt kategorilerini getirir
    public List<CategoryEntity> findByParentId(Long parentId) {
        return em.createQuery(
                "SELECT c FROM CategoryEntity c WHERE c.parentId = :pid ORDER BY c.name",
                CategoryEntity.class)
                .setParameter("pid", parentId)
                .getResultList();
    }
}
