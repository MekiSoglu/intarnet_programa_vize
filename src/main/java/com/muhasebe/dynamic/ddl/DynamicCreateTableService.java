package com.muhasebe.dynamic.ddl;

import com.muhasebe.dynamic.validator.ColumnTypeMapper;
import com.muhasebe.dynamic.validator.SqlIdentifierValidator;
import com.muhasebe.tablemap.service.TableMapService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;



import java.util.*;
import java.util.logging.Logger;

/**
 * Dinamik DDL Servisi - tablo olusturma/silme/kolon ekleme.
 */
@Stateless
public class DynamicCreateTableService {

    private static final Logger log = Logger.getLogger(DynamicCreateTableService.class.getName());

    @PersistenceContext(unitName = "muhasebePU")
    private EntityManager em;

    @Inject
    private SqlIdentifierValidator validator;

    @Inject
    private ColumnTypeMapper typeMapper;

    @Inject
    private TableMapService tableMapService;

    // ============================================================
    //  CREATE TABLE
    // ============================================================
    public void createTable(String rawTableName,
                            List<Map<String, String>> columns,
                            List<Map<String, String>> foreignKeys,
                            boolean enableAlarm,
                            Long categoryId) {

        String tableName = validator.normalize(rawTableName);
        validator.validateOrThrow(tableName, "Tablo adi");
        validator.rejectSystemTable(tableName);

        if (validator.tableExists(tableName)) {
            throw new IllegalArgumentException("Tablo zaten var: " + tableName);
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("En az bir kolon tanimlamalisiniz");
        }

        Map<String, String[]> enumMetadata = new LinkedHashMap<>();  // colName -> [values, default]

        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        sql.append(tableName).append(" (");
        sql.append("id SERIAL PRIMARY KEY, ");
        sql.append("created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        for (Map<String, String> column : columns) {
            String colName = validator.normalize(column.get("key"));
            validator.validateOrThrow(colName, "Kolon adi");
            String logicalType = column.get("type");
            String sqlType = typeMapper.toSqlType(logicalType);
            sql.append(", ").append(colName).append(" ").append(sqlType);

            if ("enum".equalsIgnoreCase(logicalType)) {
                String rawValues = column.get("enumValues");
                String defaultValue = column.get("defaultValue");
                String[] parsedValues = parseAndValidateEnumValues(rawValues);
                if (defaultValue == null || defaultValue.isBlank()) {
                    defaultValue = parsedValues[0];
                } else {
                    defaultValue = defaultValue.trim();
                    boolean found = false;
                    for (String v : parsedValues) if (v.equals(defaultValue)) { found = true; break; }
                    if (!found) {
                        throw new IllegalArgumentException(
                                "Enum default degeri '" + defaultValue + "' tanimli degerler arasinda yok: " +
                                        String.join(",", parsedValues));
                    }
                }

                StringJoiner inList = new StringJoiner(",");
                for (String v : parsedValues) inList.add("'" + v.replace("'", "''") + "'");
                sql.append(" DEFAULT '").append(defaultValue.replace("'", "''")).append("'");
                sql.append(" CHECK (").append(colName).append(" IN (").append(inList).append("))");

                enumMetadata.put(colName, new String[]{ String.join(",", parsedValues), defaultValue });
            }
        }
        sql.append(")");

        log.info("Olusturulan SQL: " + sql);
        em.createNativeQuery(sql.toString()).executeUpdate();

        //tablonun hangi kategoriye ekleneceği ör makine -> bekoloder
        Query insertQ = em.createNativeQuery(
                "INSERT INTO dynamic_tables (table_name, category_id, created_by, version) " +
                        "VALUES (?1, CAST(?2 AS BIGINT), 'system', 1)");
        insertQ.setParameter(1, tableName);
        insertQ.setParameter(2, categoryId == null ? null : categoryId.toString());
        insertQ.executeUpdate();

        for (Map.Entry<String, String[]> e : enumMetadata.entrySet()) {
            em.createNativeQuery(
                      "INSERT INTO column_enum_map (table_name, column_name, enum_values, default_value) " +
                              "VALUES (?1, ?2, ?3, ?4)")
              .setParameter(1, tableName)
              .setParameter(2, e.getKey())
              .setParameter(3, e.getValue()[0])
              .setParameter(4, e.getValue()[1])
              .executeUpdate();
        }

        if (foreignKeys != null) {
            for (Map<String, String> fk : foreignKeys) {
                processForeignKey(tableName, fk);
            }
        }
    }

    private String[] parseAndValidateEnumValues(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Enum kolon icin deger listesi bos olamaz");
        }
        String[] parts = raw.split(",");
        List<String> cleaned = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.length() > 50) {
                throw new IllegalArgumentException(
                        "Enum degeri cok uzun (max 50 karakter): '" + trimmed + "'");
            }
            cleaned.add(trimmed);
        }
        if (cleaned.size() < 2) {
            throw new IllegalArgumentException("Enum kolonu en az 2 deger icermelidir");
        }
        Set<String> unique = new HashSet<>(cleaned);
        if (unique.size() != cleaned.size()) {
            throw new IllegalArgumentException("Enum degerleri tekrar edemez");
        }
        return cleaned.toArray(new String[0]);
    }

    /** Geri uyumluluk: kategorisiz tablo. */
    public void createTable(String rawTableName,
                            List<Map<String, String>> columns,
                            List<Map<String, String>> foreignKeys,
                            boolean enableAlarm) {
        createTable(rawTableName, columns, foreignKeys, enableAlarm, null);
    }

    // tabloya fk ekleme
    private void processForeignKey(String tableName, Map<String, String> fk) {
        String relationType = fk.get("relation");
        String referencedTable = validator.normalize(fk.get("references"));
        validator.validateOrThrow(referencedTable, "Referans tablo adi");

        String relationColumn = fk.get("relationColumn");

        if (!validator.tableExists(referencedTable)) {
            createEmptyTable(referencedTable);
        }

        if ("one-to-many".equalsIgnoreCase(relationType) ||
                "many-to-one".equalsIgnoreCase(relationType)) {

            String fkColumn = validator.normalize(fk.get("column"));
            validator.validateOrThrow(fkColumn, "FK kolon adi");

            String alterSql = "ALTER TABLE " + tableName +
                    " ADD COLUMN " + fkColumn + " INTEGER REFERENCES " +
                    referencedTable + "(id) ON DELETE SET NULL";
            em.createNativeQuery(alterSql).executeUpdate();

            tableMapService.saveRelation(tableName, referencedTable, relationType,
                    relationColumn, null, fkColumn);

        } else if ("many-to-many".equalsIgnoreCase(relationType)) {

            String joinTable = tableName + "_" + referencedTable;
            if (validator.tableExists(joinTable)) {
                joinTable = joinTable + "_" + System.currentTimeMillis();
            }

            String joinSql = "CREATE TABLE " + joinTable + " (" +
                    tableName + "_id INTEGER REFERENCES " + tableName + "(id) ON DELETE CASCADE, " +
                    referencedTable + "_id INTEGER REFERENCES " + referencedTable + "(id) ON DELETE CASCADE, " +
                    "PRIMARY KEY (" + tableName + "_id, " + referencedTable + "_id))";
            em.createNativeQuery(joinSql).executeUpdate();

            tableMapService.saveRelation(tableName, referencedTable, relationType,
                    relationColumn, joinTable, null);

        } else {
            throw new IllegalArgumentException("Desteklenmeyen iliski tipi: " + relationType);
        }
    }

    //  CREATE EMPTY TABLE (FK target'i yoksa olusturmak icin)
    public void createEmptyTable(String rawTableName) {
        String tableName = validator.normalize(rawTableName);
        validator.validateOrThrow(tableName, "Tablo adi");
        validator.rejectSystemTable(tableName);

        if (validator.tableExists(tableName)) return;

        String sql = "CREATE TABLE " + tableName +
                " (id SERIAL PRIMARY KEY, created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        em.createNativeQuery(sql).executeUpdate();

        em.createNativeQuery(
                  "INSERT INTO dynamic_tables (table_name, created_by, version) " +
                          "VALUES (?1, 'system', 1)")
          .setParameter(1, tableName)
          .executeUpdate();
    }

    // ============================================================
    //  ADD / DROP COLUMN
    // ============================================================
    public void addColumnToTable(String rawTableName, String rawColumnName, String columnType) {
        String tableName = validator.normalize(rawTableName);
        String columnName = validator.normalize(rawColumnName);
        validator.validateOrThrow(tableName, "Tablo adi");
        validator.validateOrThrow(columnName, "Kolon adi");
        validator.rejectSystemTable(tableName);

        if (!validator.tableExists(tableName)) {
            throw new IllegalArgumentException("Tablo bulunamadi: " + tableName);
        }
        if (validator.columnExists(tableName, columnName)) {
            throw new IllegalArgumentException("Kolon zaten var: " + columnName);
        }

        String sqlType = typeMapper.toSqlType(columnType);
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + sqlType;
        em.createNativeQuery(sql).executeUpdate();
    }

    public void dropColumnFromTable(String rawTableName, String rawColumnName) {
        String tableName = validator.normalize(rawTableName);
        String columnName = validator.normalize(rawColumnName);
        validator.validateOrThrow(tableName, "Tablo adi");
        validator.validateOrThrow(columnName, "Kolon adi");
        validator.rejectSystemTable(tableName);

        if ("id".equals(columnName) || "created_date".equals(columnName)) {
            throw new IllegalArgumentException("Sistem kolonu silinemez: " + columnName);
        }

        if (!validator.columnExists(tableName, columnName)) {
            throw new IllegalArgumentException("Kolon bulunamadi: " + columnName);
        }

        em.createNativeQuery("ALTER TABLE " + tableName + " DROP COLUMN " + columnName)
          .executeUpdate();
        em.createNativeQuery("DELETE FROM column_enum_map WHERE table_name = ?1 AND column_name = ?2")
          .setParameter(1, tableName)
          .setParameter(2, columnName)
          .executeUpdate();
    }

    // ============================================================
    //  DROP TABLE
    // ============================================================
    public String dropTable(String rawTableName) {
        String tableName = validator.normalize(rawTableName);
        validator.validateOrThrow(tableName, "Tablo adi");
        validator.rejectSystemTable(tableName);

        if (!validator.tableExists(tableName)) {
            return "Tablo bulunamadi: " + tableName;
        }

        tableMapService.deleteAllRelationsOf(tableName);

        em.createNativeQuery("DELETE FROM column_enum_map WHERE table_name = ?1")
          .setParameter(1, tableName)
          .executeUpdate();

        em.createNativeQuery("DELETE FROM dynamic_tables WHERE table_name = ?1")
          .setParameter(1, tableName)
          .executeUpdate();

        em.createNativeQuery("DROP TABLE IF EXISTS " + tableName + " CASCADE")
          .executeUpdate();

        return "Tablo basariyla silindi: " + tableName;
    }

    //  LISTING / METADATA
    @SuppressWarnings("unchecked")
    public List<String> listAllTables() {
        return em.createNativeQuery(
                         "SELECT table_name FROM dynamic_tables ORDER BY table_name")
                 .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> listDynamicTables() {
        return em.createNativeQuery(
                         "SELECT table_name FROM dynamic_tables ORDER BY table_name")
                 .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> listTablesByCategory(Long categoryId) {
        if (categoryId == null) return java.util.Collections.emptyList();
        return em.createNativeQuery(
                         "SELECT table_name FROM dynamic_tables WHERE category_id = ?1 ORDER BY table_name")
                 .setParameter(1, categoryId)
                 .getResultList();
    }

    public Long getCategoryIdOfTable(String tableName) {
        try {
            Object result = em.createNativeQuery(
                                      "SELECT category_id FROM dynamic_tables WHERE table_name = ?1")
                              .setParameter(1, tableName)
                              .getSingleResult();
            return result == null ? null : ((Number) result).longValue();
        } catch (Exception e) {
            return null;
        }
    }

    public int dropAllTablesOfCategory(Long categoryId) {
        List<String> tables = listTablesByCategory(categoryId);
        int count = 0;
        for (String t : tables) {
            try {
                dropTable(t);
                count++;
            } catch (Exception e) {
                log.warning("Tablo silinemedi: " + t + " - " + e.getMessage());
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public List<String> getTableColumns(String rawTableName) {
        String tableName = validator.normalize(rawTableName);
        validator.validateOrThrow(tableName, "Tablo adi");
        return em.createNativeQuery(
                         "SELECT column_name FROM information_schema.columns " +
                                 "WHERE table_schema = 'public' AND table_name = ?1 ORDER BY ordinal_position")
                 .setParameter(1, tableName)
                 .getResultList();
    }


    //enum değeri güncelle
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getEnumColumns(String rawTableName) {
        String tableName = validator.normalize(rawTableName);
        validator.validateOrThrow(tableName, "Tablo adi");

        Map<String, List<String>> result = new LinkedHashMap<>();
        List<Object[]> rows = em.createNativeQuery(
                                        "SELECT column_name, enum_values FROM column_enum_map WHERE table_name = ?1")
                                .setParameter(1, tableName)
                                .getResultList();

        for (Object[] row : rows) {
            String col = (String) row[0];
            String csv = (String) row[1];
            List<String> values = new ArrayList<>();
            for (String v : csv.split(",")) {
                String t = v.trim();
                if (!t.isEmpty()) values.add(t);
            }
            result.put(col, values);
        }
        return result;
    }
}