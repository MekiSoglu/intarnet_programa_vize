package com.muhasebe.dynamic.query;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Basit dinamik sorgu servisi: tableName + columnName + value -> SELECT * WHERE col = value.
 *
 * Eski koddaki DynamicQueryService'in temizlenmis versiyonu.
 * BUG FIX: M2M FK sorgusunda join_table_name metadata'dan aliniyor (tahmin yok).
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class DynamicQueryService {

    @PersistenceContext(unitName = "muhasebePU")
    private EntityManager em;

    @Inject
    private SqlIdentifierValidator validator;

    @Inject
    private TableMapRepository tableMapRepository;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> executeDynamicQuery(String rawTableName,
                                                         String rawColumnName,
                                                         String value) {
        String tableName = validator.normalize(rawTableName);
        String columnName = validator.normalize(rawColumnName);
        validator.validateOrThrow(tableName, "Tablo adi");
        validator.validateOrThrow(columnName, "Kolon adi");

        Object typedValue = convertValueType(value);

        // M2M FK kontrolu
        if (columnName.endsWith("_id")) {
            String relatedTable = columnName.substring(0, columnName.length() - 3);
            Optional<TableMapEntity> relOpt = tableMapRepository.findRelation(tableName, relatedTable);
            if (relOpt.isPresent() &&
                "many-to-many".equalsIgnoreCase(relOpt.get().getRelationType())) {

                TableMapEntity rel = relOpt.get();
                String joinTable = rel.getJoinTableName();
                if (joinTable != null && validator.isValid(joinTable)) {
                    String mainCol, lookupCol;
                    if (rel.getTableName().equals(tableName)) {
                        mainCol = tableName + "_id";
                        lookupCol = relatedTable + "_id";
                    } else {
                        mainCol = relatedTable + "_id";
                        lookupCol = tableName + "_id";
                    }

                    String sql = "SELECT main.* FROM " + tableName + " main WHERE main.id IN (" +
                            "SELECT " + mainCol + " FROM " + joinTable + " WHERE " + lookupCol + " = ?1)";
                    return queryToMaps(sql, typedValue);
                }
            }
        }

        // Normal sorgu
        String sql = "SELECT * FROM " + tableName + " WHERE " + columnName + " = :v";
        return queryToMaps(sql, typedValue);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryToMaps(String sql, Object value) {
        Query q = em.createNativeQuery(sql);
        q.setParameter(1, value);
        List<Object[]> rows = q.getResultList();
        if (rows.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (item instanceof Object[]) {
                Object[] arr = (Object[]) item;
                for (int i = 0; i < arr.length; i++) {
                    m.put("col_" + i, arr[i]);
                }
            } else {
                m.put("col_0", item);
            }
            result.add(m);
        }
        return result;
    }

    private Object convertValueType(String value) {
        if (value == null) return null;
        // Integer mi?
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        // Date mi?
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception ignored) {}
        // Boolean mu?
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }
}
