package com.muhasebe.tablemap.service;

import com.muhasebe.base.repository.BaseRepository;
import com.muhasebe.base.service.BaseService;
import com.muhasebe.tablemap.domain.TableMapEntity;
import com.muhasebe.tablemap.repository.TableMapRepository;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tablo iliski metadatasi servisi.
 *
 * @Stateless EJB: Container-Managed Transaction (CMT) sayesinde her metod otomatik
 * transaction icinde calisir. Spring'deki @Transactional'in EJB karsiligi.
 */
@Stateless
public class TableMapService extends BaseService<TableMapEntity, TableMapRepository> {

    @Inject
    private TableMapRepository repository;

    @Override
    protected TableMapRepository getRepository() {
        return repository;
    }

    /** Verilen tablonun iliski tiplerini sirayla getir. */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getRelationTypes(String tableName) {
        return repository.findByTableName(tableName).stream()
                .map(TableMapEntity::getRelationType)
                .collect(Collectors.toList());
    }

    /** Verilen tablonun bagli tablolarini sirayla getir. */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getRelatedTables(String tableName) {
        return repository.findByTableName(tableName).stream()
                .map(TableMapEntity::getRelatedTable)
                .collect(Collectors.toList());
    }

    /** Yeni iliski kaydi olustur. */
    public TableMapEntity saveRelation(String tableName, String relatedTable,
                                        String relationType, String relationColumn,
                                        String joinTableName, String fkColumnName) {
        TableMapEntity e = new TableMapEntity();
        e.setTableName(tableName);
        e.setRelatedTable(relatedTable);
        e.setRelationType(relationType);
        e.setRelationColumName(relationColumn);
        e.setJoinTableName(joinTableName);
        e.setFkColumnName(fkColumnName);
        return repository.save(e);
    }

    /** Bir tablo silindiginde tum iliskilerini temizle. */
    public int deleteAllRelationsOf(String tableName) {
        return repository.deleteByTableName(tableName);
    }

    /** Tum iliskileri getir (UI listesi icin). */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<TableMapEntity> findAllRelations() {
        return repository.findAll();
    }
}
