package com.muhasebe.base.service;

import com.muhasebe.base.domain.BaseEntity;
import com.muhasebe.base.repository.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Generic CRUD service. Concrete service'ler bunu extend edip kendi repository'sini saglar.
 *
 * BUG FIX (eski koda gore):
 *  - Spring'in @Service yerine subclass'lar @Stateless EJB olacak (ornek: CategoryService).
 *  - Eski koddaki bos if blocklari (existsById sonrasi) kaldirildi -> dogru exception firlatiyor.
 *  - Cache annotation'lari kaldirildi.
 */
public abstract class BaseService<E extends BaseEntity, R extends BaseRepository<E>> {

    protected static final Logger log = Logger.getLogger(BaseService.class.getName());

    protected abstract R getRepository();

    public Optional<E> findOne(Long id) {
        return getRepository().findById(id);
    }

    public E getOneOrFail(Long id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot find " + getEntityClassName() + " with id: " + id));
    }

    public boolean existsById(Long id) {
        return getRepository().existsById(id);
    }

    public List<E> findAll() {
        return getRepository().findAll();
    }

    public long count() {
        return getRepository().count();
    }

    public E create(E entity) {
        if (entity.getId() != null) {
            throw new IllegalArgumentException(
                    "New " + getEntityClassName() + " cannot have an ID set");
        }
        return getRepository().save(entity);
    }

    public E update(E entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException(
                    "Cannot update " + getEntityClassName() + " without ID");
        }
        if (!existsById(entity.getId())) {
            throw new IllegalArgumentException(
                    getEntityClassName() + " with id " + entity.getId() + " does not exist");
        }
        return getRepository().update(entity);
    }

    public void delete(Long id) {
        if (!existsById(id)) {
            throw new IllegalArgumentException(
                    getEntityClassName() + " with id " + id + " does not exist");
        }
        getRepository().deleteById(id);
    }

    public String getEntityClassName() {
        return getRepository().getEntityClass().getSimpleName();
    }
}
