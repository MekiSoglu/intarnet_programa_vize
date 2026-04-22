package com.muhasebe.base.repository;

import com.muhasebe.base.domain.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD DAO. Tum repository'ler bunu extend eder.
 *
 * BUG FIX (eski koda gore):
 *  - Spring Data JpaRepository'nin yerini DAO pattern aliyor.
 *  - Cache annotation'lari kaldirildi (eski kodda @Cacheable yer yer NPE'ye sebep oluyordu).
 *  - Tum sorgular CriteriaBuilder veya named query ile -> SQL injection guvenli.
 *
 * Subclass kullanim ornegi:
 *   {@code
 *   @ApplicationScoped
 *   public class CategoryRepository extends BaseRepository<CategoryEntity> {
 *       public CategoryRepository() { super(CategoryEntity.class); }
 *   }
 *   }
 */
public abstract class BaseRepository<E extends BaseEntity> {

    @PersistenceContext(unitName = "muhasebePU")
    protected EntityManager em;

    private final Class<E> entityClass;

    protected BaseRepository(Class<E> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<E> getEntityClass() {
        return entityClass;
    }

    public EntityManager getEntityManager() {
        return em;
    }

    // ---------- READ ----------

    public Optional<E> findById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(em.find(entityClass, id));
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    public List<E> findAll() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<E> q = cb.createQuery(entityClass);
        Root<E> root = q.from(entityClass);
        q.select(root);
        return em.createQuery(q).getResultList();
    }

    public long count() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        q.select(cb.count(q.from(entityClass)));
        return em.createQuery(q).getSingleResult();
    }

    // ---------- WRITE ----------

    public E save(E entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public E update(E entity) {
        return em.merge(entity);
    }

    public void delete(E entity) {
        E managed = em.contains(entity) ? entity : em.merge(entity);
        em.remove(managed);
    }

    public void deleteById(Long id) {
        findById(id).ifPresent(this::delete);
    }

    public void deleteAll() {
        em.createQuery("DELETE FROM " + entityClass.getSimpleName() + " e").executeUpdate();
    }

    // ---------- UTIL ----------

    public void flush() {
        em.flush();
    }

    public void detach(E entity) {
        em.detach(entity);
    }

    public E attach(E entity) {
        if (em.contains(entity)) return entity;
        if (entity.getId() == null) {
            throw new IllegalStateException("Cannot attach entity with null ID");
        }
        return em.merge(entity);
    }
}
