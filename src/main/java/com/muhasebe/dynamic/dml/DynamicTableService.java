package com.muhasebe.dynamic.dml;

import com.muhasebe.dynamic.validator.SqlIdentifierValidator;
import com.muhasebe.tablemap.domain.TableMapEntity;
import com.muhasebe.tablemap.repository.TableMapRepository;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.*;
import java.util.logging.Logger;

@Stateless
public class DynamicTableService {

    private static final Logger log = Logger.getLogger(DynamicTableService.class.getName());

    @PersistenceContext(unitName = "muhasebePU")
    private EntityManager em;

    @Inject
    private SqlIdentifierValidator validator;

    @Inject
    private TableMapRepository tableMapRepository;

    //  INSERT
    public Long insertIntoTable(String rawTableName, Map<String, Object> values) {
        String tableName = validator.normalize(rawTableName);
        validator.validateOrThrow(tableName, "Tablo adi");
        validator.rejectSystemTable(tableName);

        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Eklenecek deger bulunamadi");
        }

        // 1. Tablonun kolon tiplerini veritabanından çek
        Map<String, String> columnTypes = new HashMap<>();
        List<Object[]> typeResults = em.createNativeQuery(
                                               "SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ?")
                                       .setParameter(1, tableName)
                                       .getResultList();
        for (Object[] row : typeResults) {
            columnTypes.put((String) row[0], (String) row[1]);
        }

        List<TableMapEntity> relations = tableMapRepository.findByTableName(tableName);
        Map<String, TableMapEntity> manyToManyMap = new HashMap<>();
        for (TableMapEntity r : relations) {
            if ("many-to-many".equalsIgnoreCase(r.getRelationType())) {
                manyToManyMap.put(r.getRelatedTable() + "_id", r);
            }
        }

        Map<String, Object> normalColumns = new LinkedHashMap<>();
        Map<TableMapEntity, List<Long>> manyToManyValues = new HashMap<>();

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String col = validator.normalize(entry.getKey());
            validator.validateOrThrow(col, "Kolon adi");

            if (manyToManyMap.containsKey(col)) {
                TableMapEntity relation = manyToManyMap.get(col);
                List<Long> ids = extractIdList(entry.getValue());
                manyToManyValues.put(relation, ids);
            } else {
                // Değeri kolon tipine göre dönüştür
                normalColumns.put(col, convertToTargetType(entry.getValue(), columnTypes.get(col)));
            }
        }

        Long insertedId;
        if (normalColumns.isEmpty()) {
            Object id = em.createNativeQuery(
                                  "INSERT INTO " + tableName + " DEFAULT VALUES RETURNING id")
                          .getSingleResult();
            insertedId = ((Number) id).longValue();
        } else {
            StringJoiner cols = new StringJoiner(", ");
            StringJoiner placeholders = new StringJoiner(", ");
            int i = 1;
            for (String col : normalColumns.keySet()) {
                cols.add(col);
                placeholders.add("?" + i);
                i++;
            }
            String sql = "INSERT INTO " + tableName + " (" + cols + ") VALUES (" + placeholders + ") RETURNING id";
            Query q = em.createNativeQuery(sql);

            i = 1;
            for (Object value : normalColumns.values()) {
                q.setParameter(i, value);
                i++;
            }
            insertedId = ((Number) q.getSingleResult()).longValue();
        }

        for (Map.Entry<TableMapEntity, List<Long>> entry : manyToManyValues.entrySet()) {
            insertManyToMany(entry.getKey(), insertedId, entry.getValue());
        }

        return insertedId;
    }

    // Yardımcı metod: String gelen veriyi DB tipine göre objeye çevirir
    private Object convertToTargetType(Object value, String dataType) {
        if (value == null || dataType == null || value.toString().isEmpty()) return null;

        String strVal = value.toString().trim();
        try {
            switch (dataType.toLowerCase()) {
                case "integer":
                case "int4":
                case "serial":
                    return Integer.parseInt(strVal);
                case "bigint":
                case "int8":
                case "bigserial":
                    return Long.parseLong(strVal);
                case "numeric":
                case "decimal":
                case "real":
                case "double precision":
                    return new java.math.BigDecimal(strVal);
                case "boolean":
                    return Boolean.parseBoolean(strVal);
                case "timestamp":
                case "timestamp without time zone":
                    // Gelen tarih formatına göre burası düzenlenebilir
                    return value;
                default:
                    return value;
            }
        } catch (NumberFormatException e) {
            // Loglama yapılabilir: Sayı beklenen kolona metin girilmiş
            return value;
        }
    }

    //many to many de ara tabloya veri atar
    public void insertManyToMany(TableMapEntity relation, Long mainId, List<Long> relatedIds) {
        String joinTable = relation.getJoinTableName();
        if (joinTable == null) {
            throw new IllegalStateException("join_table_name metadata'da tanimsiz: " +
                    relation.getTableName() + " - " + relation.getRelatedTable());
        }
        validator.validateOrThrow(joinTable, "Join tablo adi");

        String mainCol = relation.getTableName() + "_id";
        String relatedCol = relation.getRelatedTable() + "_id";

        for (Long relId : relatedIds) {
            em.createNativeQuery(
                      "INSERT INTO " + joinTable + " (" + mainCol + ", " + relatedCol + ") " +
                              "VALUES (?1, ?2) ON CONFLICT DO NOTHING")
              .setParameter(1, mainId)
              .setParameter(2, relId)
              .executeUpdate();
        }
    }

    private List<Long> extractIdList(Object value) {
        List<Long> ids = new ArrayList<>();
        if (value == null) return ids;

        if (value instanceof Collection<?>) {
            for (Object item : (Collection<?>) value) {
                addAsLong(ids, item);
            }
        } else {
            addAsLong(ids, value);
        }
        return ids;
    }

    // güvenli long dönüşümü
    private void addAsLong(List<Long> list, Object item) {
        if (item == null) return;
        if (item instanceof Number) {
            list.add(((Number) item).longValue());
        } else if (item instanceof Map) {
            Object id = ((Map<?, ?>) item).get("id");
            if (id != null) addAsLong(list, id);
        } else {
            try {
                list.add(Long.parseLong(item.toString()));
            } catch (NumberFormatException e) {
                log.warning("ID olarak parse edilemedi: " + item);
            }
        }
    }


    public void updateTable(String rawTableName, Long id, Map<String, Object> updates) {
        String tableName = validator.normalize(rawTableName);
        validator.validateOrThrow(tableName, "Tablo adi");
        validator.rejectSystemTable(tableName);

        if (id == null) throw new IllegalArgumentException("ID bos olamaz");
        if (updates == null || updates.isEmpty()) return;

        List<TableMapEntity> relations = tableMapRepository.findByTableName(tableName);
        Map<String, TableMapEntity> manyToManyMap = new HashMap<>();
        for (TableMapEntity r : relations) {
            if ("many-to-many".equalsIgnoreCase(r.getRelationType())) {
                manyToManyMap.put(r.getRelatedTable() + "_id", r);
            }
        }

        Map<String, Object> normalColumns = new LinkedHashMap<>();
        Map<TableMapEntity, List<Long>> manyToManyUpdates = new HashMap<>();

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String col = validator.normalize(entry.getKey());
            validator.validateOrThrow(col, "Kolon adi");

            if (manyToManyMap.containsKey(col)) {
                TableMapEntity rel = manyToManyMap.get(col);
                manyToManyUpdates.put(rel, extractIdList(entry.getValue()));
            } else {
                normalColumns.put(col, entry.getValue());
            }
        }

        if (!normalColumns.isEmpty()) {
            StringJoiner setClause = new StringJoiner(", ");
            int i = 1;
            for (String col : normalColumns.keySet()) {
                setClause.add(col + " = ?" + i);
                i++;
            }
            // Son parametre id olacak
            String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE id = ?" + i;
            Query q = em.createNativeQuery(sql);
            int p = 1;
            for (Object v : normalColumns.values()) {
                q.setParameter(p, v);
                p++;
            }
            q.setParameter(p, id);
            q.executeUpdate();
        }

        for (Map.Entry<TableMapEntity, List<Long>> entry : manyToManyUpdates.entrySet()) {
            TableMapEntity rel = entry.getKey();
            String joinTable = rel.getJoinTableName();
            if (joinTable == null) continue;
            validator.validateOrThrow(joinTable, "Join tablo adi");

            em.createNativeQuery(
                      "DELETE FROM " + joinTable + " WHERE " + tableName + "_id = ?1")
              .setParameter(1, id)
              .executeUpdate();

            insertManyToMany(rel, id, entry.getValue());
        }
    }


    public void deleteFromTable(String rawTableName, Long id) {
        String tableName = validator.normalize(rawTableName);
        validator.validateOrThrow(tableName, "Tablo adi");
        validator.rejectSystemTable(tableName);
        if (id == null) throw new IllegalArgumentException("ID bos olamaz");

        em.createNativeQuery("DELETE FROM " + tableName + " WHERE id = ?1")
          .setParameter(1, id)
          .executeUpdate();
    }

    // tablo verileri
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTableData(String rawTableName) {
        String tableName = validator.normalize(rawTableName);
        validator.validateOrThrow(tableName, "Tablo adi");

        List<String> columnNames = getTableColumns(tableName);
        List<TableMapEntity> relations = tableMapRepository.findByTableName(tableName);

        Map<String, TableMapEntity> manyToOneRelations = new HashMap<>();
        Map<String, TableMapEntity> manyToManyRelations = new HashMap<>();
        for (TableMapEntity r : relations) {
            if ("one-to-many".equalsIgnoreCase(r.getRelationType()) ||
                    "many-to-one".equalsIgnoreCase(r.getRelationType())) {
                manyToOneRelations.put(r.getRelatedTable(), r);
            } else if ("many-to-many".equalsIgnoreCase(r.getRelationType())) {
                manyToManyRelations.put(r.getRelatedTable(), r);
            }
        }

        List<Object[]> rawData = em.createNativeQuery("SELECT * FROM " + tableName).getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawData) {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            // Tek kolonlu tabloda Object[] degil direkt Object dondurebilir
            if (item instanceof Object[]) {
                Object[] row = (Object[]) item;
                for (int i = 0; i < columnNames.size() && i < row.length; i++) {
                    rowMap.put(columnNames.get(i), row[i]);
                }
            } else {
                rowMap.put(columnNames.get(0), item);
            }

            Object idObj = rowMap.get("id");
            if (idObj != null) {
                resolveManyToOne(rowMap, manyToOneRelations);
                resolveManyToMany(rowMap, tableName, ((Number) idObj).longValue(), manyToManyRelations);
            }

            result.add(rowMap);
        }
        return result;
    }

    // fk değeri ilşki kurlurken istenilen satır adına dönüştürülür
    @SuppressWarnings("unchecked")
    private void resolveManyToOne(Map<String, Object> rowMap,
                                  Map<String, TableMapEntity> m2oRelations) {
        for (TableMapEntity rel : m2oRelations.values()) {
            String fkCol = rel.getFkColumnName();
            String displayCol = rel.getRelationColumName();
            if (fkCol == null || displayCol == null) continue;
            if (!validator.isValid(fkCol) || !validator.isValid(displayCol)) continue;

            Object fkValue = rowMap.get(fkCol);
            if (fkValue == null) continue;

            try {
                List<Object> displayValues = em.createNativeQuery(
                                                       "SELECT " + displayCol + " FROM " + rel.getRelatedTable() + " WHERE id = ?1")
                                               .setParameter(1, ((Number) fkValue).longValue())
                                               .getResultList();
                if (!displayValues.isEmpty()) {
                    // Ham ID'yi yedekle (edit dialog'unda dropdown icin lazim)
                    rowMap.put(fkCol + "_raw", fkValue);
                    // FK kolonunun kendisini display metniyle degistir
                    rowMap.put(fkCol, displayValues.get(0));
                }
            } catch (Exception e) {
                log.warning("M2O resolve hatasi: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void resolveManyToMany(Map<String, Object> rowMap, String tableName, Long id,
                                   Map<String, TableMapEntity> m2mRelations) {
        for (TableMapEntity rel : m2mRelations.values()) {
            String joinTable = rel.getJoinTableName();
            String displayCol = rel.getRelationColumName();
            if (joinTable == null || displayCol == null) continue;
            if (!validator.isValid(joinTable) || !validator.isValid(displayCol)) continue;

            try {
                String relatedTable = rel.getRelatedTable();
                String sql = "SELECT " + relatedTable + "." + displayCol +
                        " FROM " + relatedTable +
                        " JOIN " + joinTable + " ON " + relatedTable + ".id = " +
                        joinTable + "." + relatedTable + "_id" +
                        " WHERE " + joinTable + "." + tableName + "_id = ?1";

                List<Object> values = em.createNativeQuery(sql)
                                        .setParameter(1, id)
                                        .getResultList();

                List<String> strings = new ArrayList<>();
                for (Object v : values) {
                    if (v != null) strings.add(v.toString());
                }
                rowMap.put(relatedTable, String.join(", ", strings));
            } catch (Exception e) {
                log.warning("M2M resolve hatasi: " + e.getMessage());
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @SuppressWarnings("unchecked")
    public List<String> getTableColumns(String rawTableName) {
        String tableName = validator.normalize(rawTableName);
        return em.createNativeQuery(
                         "SELECT column_name FROM information_schema.columns " +
                                 "WHERE table_schema = 'public' AND table_name = ?1 ORDER BY ordinal_position")
                 .setParameter(1, tableName)
                 .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getForeignKeyData(String rawRelatedTable, String rawDisplayColumn) {
        String relatedTable = validator.normalize(rawRelatedTable);
        String displayColumn = validator.normalize(rawDisplayColumn);
        validator.validateOrThrow(relatedTable, "Tablo adi");
        validator.validateOrThrow(displayColumn, "Kolon adi");

        List<Object[]> rows = em.createNativeQuery(
                                        "SELECT id, " + displayColumn + " AS value FROM " + relatedTable)
                                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", row[0]);
            m.put("value", row[1]);
            result.add(m);
        }
        return result;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Map<String, Object>> getForeignKeys(String rawTableName) {
        String tableName = validator.normalize(rawTableName);
        List<TableMapEntity> relations = tableMapRepository.findByTableName(tableName);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TableMapEntity r : relations) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("relatedTable", r.getRelatedTable());
            m.put("relationColumn", r.getRelationColumName());
            m.put("relationType", r.getRelationType());
            m.put("fkColumn", r.getFkColumnName());
            m.put("joinTable", r.getJoinTableName());
            result.add(m);
        }
        return result;
    }
}