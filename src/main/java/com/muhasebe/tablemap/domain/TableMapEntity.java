package com.muhasebe.tablemap.domain;

import com.muhasebe.base.domain.BaseAuditingEntity;
import jakarta.persistence.*;

//tablolar arası ilişki burada saklanır metadatan almak riskli
@Entity
@Table(name = "table_map")
public class TableMapEntity extends BaseAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 255)
    private String tableName;

    @Column(name = "related_table", nullable = false, length = 255)
    private String relatedTable;

    @Column(name = "relation_type", nullable = false, length = 50)
    private String relationType;   // "one-to-many", "many-to-many"

    @Column(name = "relation_colum_name", length = 255)
    private String relationColumName;  // hangi kolonu UI'da gosterelim

    /** YENI: many-to-many icin gercek ara tablo adi (ambiguity'yi cozer). */
    @Column(name = "join_table_name", length = 255)
    private String joinTableName;

    /** YENI: one-to-many icin FK kolonunun gercek adi. */
    @Column(name = "fk_column_name", length = 255)
    private String fkColumnName;

    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getRelatedTable() { return relatedTable; }
    public void setRelatedTable(String relatedTable) { this.relatedTable = relatedTable; }

    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }

    public String getRelationColumName() { return relationColumName; }
    public void setRelationColumName(String relationColumName) { this.relationColumName = relationColumName; }

    public String getJoinTableName() { return joinTableName; }
    public void setJoinTableName(String joinTableName) { this.joinTableName = joinTableName; }

    public String getFkColumnName() { return fkColumnName; }
    public void setFkColumnName(String fkColumnName) { this.fkColumnName = fkColumnName; }
}
