package com.muhasebe.dynamic.join;

import com.muhasebe.dynamic.validator.SqlIdentifierValidator;
import com.muhasebe.tablemap.domain.TableMapEntity;
import com.muhasebe.tablemap.domain.ViewMap;
import com.muhasebe.tablemap.repository.TableMapRepository;
import com.muhasebe.tablemap.service.ViewMapService;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Dinamik View Olusturma + Filtreleme Servisi.
 * * KRITIK DUZELTMELER:
 * 1. "missing FROM-clause" hatasi giderildi: JOIN edilmeyen tablolar artik FROM kismina ekleniyor.
 * 2. fetchFilteredDataFromView metodu eklendi ve CAST(AS TEXT) ile guclendirildi.
 * 3. DROP VIEW IF EXISTS eklendi.
 */
@Stateless
public class DynamicJoinViewService {

    private static final Logger log = Logger.getLogger(DynamicJoinViewService.class.getName());

    @PersistenceContext(unitName = "muhasebePU")
    private EntityManager em;

    @Inject
    private SqlIdentifierValidator validator;

    @Inject
    private TableMapRepository tableMapRepository;

    @Inject
    private ViewMapService viewMapService;

    private static final Set<String> HIDDEN_COLUMN_SUFFIXES = Set.of(
            "_id", "_created_by", "_last_modified_by",
            "_created_date", "_last_modified_date", "_version"
    );

    private static final Set<String> HIDDEN_COLUMNS = Set.of(
            "created_by", "last_modified_by", "created_date",
            "last_modified_date", "version", "date_create"
    );

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Map<String, Object>> getTableRelations(List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            throw new IllegalArgumentException("Tablo listesi bos olamaz");
        }

        List<Map<String, Object>> relations = new ArrayList<>();
        Set<String> tableSet = tableNames.stream()
                                         .map(validator::normalize)
                                         .collect(Collectors.toSet());
        Set<String> seenPairs = new HashSet<>();

        for (String tn : tableSet) {
            for (TableMapEntity rel : tableMapRepository.findByTableName(tn)) {
                if (tableSet.contains(rel.getRelatedTable())) {
                    if (seenPairs.add(pairKey(rel.getTableName(), rel.getRelatedTable()))) {
                        relations.add(toMap(rel));
                    }
                }
            }
            for (TableMapEntity rel : tableMapRepository.findByRelatedTable(tn)) {
                if (tableSet.contains(rel.getTableName())) {
                    if (seenPairs.add(pairKey(rel.getTableName(), rel.getRelatedTable()))) {
                        relations.add(toMap(rel));
                    }
                }
            }
        }
        return relations;
    }

    private String pairKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "::" + b : b + "::" + a;
    }

    private Map<String, Object> toMap(TableMapEntity rel) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("table_name", rel.getTableName());
        m.put("related_table", rel.getRelatedTable());
        m.put("relation_type", rel.getRelationType());
        m.put("join_table_name", rel.getJoinTableName());
        m.put("fk_column_name", rel.getFkColumnName());
        return m;
    }

    /**
     * View olusturma mantigi. Tablolar arasi iliski yoksa Cross Join yapar.
     */
    public String createDynamicView(List<String> tableNames,
                                    List<Map<String, Object>> relations,
                                    String rawViewName) {
        if (tableNames == null || tableNames.isEmpty()) {
            throw new IllegalArgumentException("En az bir tablo secmelisiniz");
        }

        String viewName = validator.normalize(rawViewName);
        validator.validateOrThrow(viewName, "View adi");
        validator.rejectSystemTable(viewName);

        // Eski view varsa temizle
        em.createNativeQuery("DROP VIEW IF EXISTS " + viewName).executeUpdate();

        List<String> normalizedTables = tableNames.stream()
                                                  .map(validator::normalize)
                                                  .peek(n -> {
                                                      if (!validator.tableExists(n)) throw new IllegalArgumentException("Tablo bulunamadi: " + n);
                                                  })
                                                  .collect(Collectors.toList());

        // SELECT kismi
        StringBuilder sql = new StringBuilder("CREATE VIEW ").append(viewName).append(" AS SELECT ");
        StringJoiner selectJoiner = new StringJoiner(", ");
        for (String t : normalizedTables) {
            for (String col : getColumnsOf(t)) {
                selectJoiner.add(t + "." + col + " AS " + t + "_" + col);
            }
        }
        sql.append(selectJoiner);

        // FROM kismi (Ilk tablo)
        sql.append(" FROM ").append(normalizedTables.get(0));

        Set<String> joinedTables = new HashSet<>();
        joinedTables.add(normalizedTables.get(0));

        // JOIN islemleri
        if (relations != null) {
            for (Map<String, Object> rel : relations) {
                String tableA = validator.normalize(String.valueOf(rel.get("table_name")));
                String tableB = validator.normalize(String.valueOf(rel.get("related_table")));
                String relType = String.valueOf(rel.get("relation_type"));

                String newTable, existingTable;
                if (joinedTables.contains(tableA) && !joinedTables.contains(tableB)) {
                    existingTable = tableA; newTable = tableB;
                } else if (joinedTables.contains(tableB) && !joinedTables.contains(tableA)) {
                    existingTable = tableB; newTable = tableA;
                } else continue;

                if ("many-to-many".equalsIgnoreCase(relType)) {
                    String joinTable = (String) rel.get("join_table_name");
                    if (joinTable != null) {
                        sql.append(" LEFT JOIN ").append(joinTable)
                           .append(" ON ").append(existingTable).append(".id = ").append(joinTable).append(".").append(existingTable).append("_id")
                           .append(" LEFT JOIN ").append(newTable)
                           .append(" ON ").append(newTable).append(".id = ").append(joinTable).append(".").append(newTable).append("_id");
                        joinedTables.add(newTable);
                    }
                } else {
                    TableMapEntity rawRel = tableMapRepository.findRelation(tableA, tableB).orElse(null);
                    if (rawRel != null && rawRel.getFkColumnName() != null) {
                        String fkCol = rawRel.getFkColumnName();
                        if (rawRel.getTableName().equals(newTable)) {
                            sql.append(" LEFT JOIN ").append(newTable).append(" ON ").append(newTable).append(".").append(fkCol).append(" = ").append(existingTable).append(".id");
                        } else {
                            sql.append(" LEFT JOIN ").append(newTable).append(" ON ").append(existingTable).append(".").append(fkCol).append(" = ").append(newTable).append(".id");
                        }
                        joinedTables.add(newTable);
                    }
                }
            }
        }

        // Diger tablolar (Iliskisi olmayanlar) FROM kismina virgulle eklenir
        for (String t : normalizedTables) {
            if (!joinedTables.contains(t)) {
                sql.append(", ").append(t);
                joinedTables.add(t);
            }
        }

        log.info("Final VIEW SQL: " + sql);
        em.createNativeQuery(sql.toString()).executeUpdate();

        ViewMap vm = new ViewMap();
        vm.setViewName(viewName);
        vm.setRelationTableName(String.join("_", normalizedTables));
        viewMapService.create(vm);

        return viewName;
    }

    @SuppressWarnings("unchecked")
    private List<String> getColumnsOf(String tableName) {
        return em.createNativeQuery(
                         "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ?1 ORDER BY ordinal_position")
                 .setParameter(1, tableName)
                 .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Map<String, Object>> fetchDataFromView(String rawViewName) {
        String viewName = validator.normalize(rawViewName);
        List<String> visibleColumns = getColumnsOf(viewName).stream()
                                                            .filter(col -> !isHiddenColumn(col))
                                                            .collect(Collectors.toList());

        if (visibleColumns.isEmpty()) return Collections.emptyList();

        String sql = "SELECT " + String.join(", ", visibleColumns) + " FROM " + viewName;
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        return convertToMapList(rows, visibleColumns);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Map<String, Object>> fetchFilteredDataFromView(String rawViewName, String rawColumnName, Object value) {
        String viewName = validator.normalize(rawViewName);
        String columnName = validator.normalize(rawColumnName);

        validator.validateOrThrow(viewName, "View adi");
        validator.validateOrThrow(columnName, "Kolon adi");

        List<String> visibleColumns = getColumnsOf(viewName).stream()
                                                            .filter(col -> !isHiddenColumn(col))
                                                            .collect(Collectors.toList());

        if (visibleColumns.isEmpty()) return Collections.emptyList();

        String sql = "SELECT " + String.join(", ", visibleColumns) + " FROM " + viewName +
                " WHERE CAST(" + columnName + " AS TEXT) = ?1";

        List<Object[]> rows = em.createNativeQuery(sql)
                                .setParameter(1, value == null ? null : value.toString())
                                .getResultList();

        return convertToMapList(rows, visibleColumns);
    }

    private List<Map<String, Object>> convertToMapList(List<Object[]> rows, List<String> cols) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (int i = 0; i < cols.size(); i++) {
                m.put(cols.get(i), row[i]);
            }
            result.add(m);
        }
        return result;
    }

    private boolean isHiddenColumn(String col) {
        String lower = col.toLowerCase();
        return HIDDEN_COLUMN_SUFFIXES.stream().anyMatch(lower::endsWith) ||
                HIDDEN_COLUMNS.stream().anyMatch(h -> lower.equals(h) || lower.endsWith("_" + h));
    }

    public String deleteView(String rawViewName) {
        String viewName = validator.normalize(rawViewName);
        em.createNativeQuery("DROP VIEW IF EXISTS " + viewName).executeUpdate();
        viewMapService.deleteByName(viewName);
        return "View silindi: " + viewName;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ViewMap> listAllViews() {
        return viewMapService.findAll();
    }
}