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


@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
// javada önceden isimleri belli olamayan (nesne oluştulmamş) yapılara run time da istek atamasın bu sınıf Native sql kullanark
//önceden hazırmış gibi sorgu atar gelen veriyi java nesnesi gibi dönüştürür
public class DynamicQueryService {

    @PersistenceContext(unitName = "muhasebePU")
    private EntityManager em;

    @Inject
    private SqlIdentifierValidator validator;

    @Inject
    private TableMapRepository tableMapRepository;

    // ... executeDynamicQuery metodu aynı kalabilir, sadece queryToMaps'i kolon isimlerini alacak şekilde güncelliyoruz ...

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> executeDynamicQuery(String tableName, String columnName, String value, List<String> columnNames) {
        String normalizedTable = validator.normalize(tableName);
        String normalizedColumn = validator.normalize(columnName);
        Object typedValue = convertValueType(value);

        // Basit filtreleme sorgusu
        String sql = "SELECT * FROM " + normalizedTable + " WHERE " + normalizedColumn + " = ?1";

        Query q = em.createNativeQuery(sql);
        q.setParameter(1, typedValue);
        List<Object[]> rawData = q.getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawData) {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            if (item instanceof Object[]) {
                Object[] row = (Object[]) item;
                // Verileri gerçek kolon isimleriyle eşleştiriyoruz
                for (int i = 0; i < columnNames.size() && i < row.length; i++) {
                    rowMap.put(columnNames.get(i), row[i]);
                }
            } else {
                rowMap.put(columnNames.get(0), item);
            }
            result.add(rowMap);
        }
        return result;
    }

    //sql map dönüşümü
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


    //java tipi ile sql tipini eşleştirir
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
