package com.muhasebe.tablemap.repository;

import com.muhasebe.base.repository.BaseRepository;
import com.muhasebe.tablemap.domain.TableMapEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TableMapRepository extends BaseRepository<TableMapEntity> {

    public TableMapRepository() {
        super(TableMapEntity.class);
    }

    /** Verilen tabloya ait tum iliskileri getir (table_name = ?) */
    public List<TableMapEntity> findByTableName(String tableName) {
        return em.createQuery(
                "SELECT t FROM TableMapEntity t WHERE t.tableName = :tn",
                TableMapEntity.class)
                .setParameter("tn", tableName)
                .getResultList();
    }

    /** Verilen tabloya gelen iliskileri getir (related_table = ?) */
    public List<TableMapEntity> findByRelatedTable(String relatedTable) {
        return em.createQuery(
                "SELECT t FROM TableMapEntity t WHERE t.relatedTable = :rt",
                TableMapEntity.class)
                .setParameter("rt", relatedTable)
                .getResultList();
    }

    /** Iki tablo arasindaki iliskiyi getir  */
    public Optional<TableMapEntity> findRelation(String tableA, String tableB) {
        List<TableMapEntity> result = em.createQuery(
                "SELECT t FROM TableMapEntity t " +
                "WHERE (t.tableName = :a AND t.relatedTable = :b) " +
                "   OR (t.tableName = :b AND t.relatedTable = :a)",
                TableMapEntity.class)
                .setParameter("a", tableA)
                .setParameter("b", tableB)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /** Bir tablonun tum iliskilerini sil  */
    public int deleteByTableName(String tableName) {
        return em.createQuery(
                "DELETE FROM TableMapEntity t " +
                "WHERE t.tableName = :tn OR t.relatedTable = :tn")
                .setParameter("tn", tableName)
                .executeUpdate();
    }
}
