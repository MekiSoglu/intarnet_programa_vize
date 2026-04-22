package com.muhasebe.web.bean;

import com.muhasebe.dynamic.ddl.DynamicCreateTableService;
import com.muhasebe.dynamic.dml.DynamicTableService;
import com.muhasebe.web.util.FacesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.*;

/**
 * Tablo verisi CRUD sayfasi (pages/tables/data.xhtml).
 * URL: /pages/tables/data.xhtml?table=tabloAdi
 */
@Named
@ViewScoped
public class TableDataBean implements Serializable {

    @Inject
    private DynamicTableService dmlService;

    @Inject
    private DynamicCreateTableService ddlService;

    private String tableName;
    private List<String> columns = new ArrayList<>();
    private List<Map<String, Object>> rows = new ArrayList<>();
    private Map<String, Object> newRow = new LinkedHashMap<>();

    private Map<String, Object> editRow;
    private Long editRowId;

    private Map<String, List<String>> enumColumns = new HashMap<>();


    /** FK kolonlarinin meta bilgisi: col adi -> relatedTable. */
    private Map<String, String> fkColumns = new HashMap<>();
    /** FK kolonlarinin dropdown secenekleri: col adi -> [{id, value}, ...] */
    private Map<String, List<Map<String, Object>>> fkOptions = new HashMap<>();

    @PostConstruct
    public void init() {
        tableName = FacesUtil.getRequestParam("table");
        if (tableName != null && !tableName.isBlank()) {
            loadTable();
        }
    }

    public void loadTable() {
        try {
            columns = dmlService.getTableColumns(tableName);
            rows = dmlService.getTableData(tableName);
            loadForeignKeys();
            loadEnumColumns();

            resetNewRow();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    private void loadForeignKeys() {
        fkColumns.clear();
        fkOptions.clear();
        try {
            List<Map<String, Object>> fks = dmlService.getForeignKeys(tableName);
            for (Map<String, Object> fk : fks) {
                String relType = (String) fk.get("relationType");
                String relatedTable = (String) fk.get("relatedTable");
                String displayCol = (String) fk.get("relationColumn");

                if (relatedTable == null || displayCol == null) continue;

                // M2O: FK kolon adi aliniyor
                if ("many-to-one".equalsIgnoreCase(relType) || "one-to-many".equalsIgnoreCase(relType)) {
                    String fkCol = (String) fk.get("fkColumn");
                    if (fkCol != null) {
                        fkColumns.put(fkCol, relatedTable);
                        fkOptions.put(fkCol, dmlService.getForeignKeyData(relatedTable, displayCol));
                    }
                }
                // M2M: relatedTable_id seklinde varsayiliyor
                else if ("many-to-many".equalsIgnoreCase(relType)) {
                    String fkCol = relatedTable + "_id";
                    fkColumns.put(fkCol, relatedTable);
                    fkOptions.put(fkCol, dmlService.getForeignKeyData(relatedTable, displayCol));
                    // Eger kolon listede yoksa ekle (M2M de secilebilsin)
                    if (!columns.contains(fkCol)) {
                        columns.add(fkCol);
                    }
                }
            }
        } catch (Exception e) {
            // sessizce gec - FK yoksa normal tablo
        }
    }

    private void resetNewRow() {
        newRow = new LinkedHashMap<>();
        for (String col : columns) {
            if (!"id".equals(col) && !"created_date".equals(col)) {
                newRow.put(col, "");
            }
        }
    }

    public void addRow() {
        try {
            Map<String, Object> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : newRow.entrySet()) {
                if (e.getValue() != null && !e.getValue().toString().isBlank()) {
                    filtered.put(e.getKey(), e.getValue());
                }
            }
            if (filtered.isEmpty()) {
                FacesUtil.warn("En az bir alan doldurun");
                return;
            }
            dmlService.insertIntoTable(tableName, filtered);
            FacesUtil.info("Kayit eklendi");
            loadTable();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void deleteRow(Map<String, Object> row) {
        try {
            Object idObj = row.get("id");
            if (idObj == null) {
                FacesUtil.error("Bu satirin ID'si yok");
                return;
            }
            dmlService.deleteFromTable(tableName, ((Number) idObj).longValue());
            FacesUtil.info("Kayit silindi");
            loadTable();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void prepareEdit(Map<String, Object> row) {
        editRow = new LinkedHashMap<>(row);
        Object idObj = row.get("id");
        editRowId = idObj == null ? null : ((Number) idObj).longValue();

        // FK kolonlarinin display metni yerine ham ID'yi koy (dropdown'da secili gostermek icin)
        // getTableData() her FK kolonu icin "<fkCol>_raw" seklinde ham ID yedekliyor.
        for (String fkCol : fkColumns.keySet()) {
            Object raw = row.get(fkCol + "_raw");
            if (raw != null) {
                editRow.put(fkCol, raw);
            }
        }
    }

    public void saveEdit() {
        if (editRow == null || editRowId == null) return;
        try {
            Map<String, Object> updates = new LinkedHashMap<>(editRow);
            updates.remove("id");
            updates.remove("created_date");
            // "_raw" suffix'li gecici alanlari cikar (DB'de kolon degiller)
            updates.keySet().removeIf(k -> k.endsWith("_raw"));
            dmlService.updateTable(tableName, editRowId, updates);
            FacesUtil.info("Kayit guncellendi");
            editRow = null;
            editRowId = null;
            loadTable();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    private void loadEnumColumns() {
        try {
            enumColumns = ddlService.getEnumColumns(tableName);
        } catch (Exception e) {
            enumColumns = new HashMap<>();
        }
    }

    public void cancelEdit() {
        editRow = null;
        editRowId = null;
    }


    public boolean isEnumCol(String col) {
        return enumColumns.containsKey(col);
    }

    public List<String> getEnumValuesOf(String col) {
        return enumColumns.getOrDefault(col, Collections.emptyList());
    }

    public Map<String, List<String>> getEnumColumns() {
        return enumColumns;
    }

    /** XHTML'den cagrilir: bu kolon FK mi? */
    public boolean isForeignKey(String col) {
        return fkColumns.containsKey(col);
    }

    public List<Map<String, Object>> getOptionsFor(String col) {
        return fkOptions.getOrDefault(col, Collections.emptyList());
    }

    public boolean isBooleanCol(String col) {
        if (col == null) return false;
        String lower = col.toLowerCase();
        return lower.contains("status") || lower.startsWith("is_")
            || lower.contains("active") || lower.contains("enabled");
    }

    public boolean isDateCol(String col) {
        return col != null && col.toLowerCase().contains("date");
    }

    public boolean isSystemCol(String col) {
        return "id".equals(col) || "created_date".equals(col);
    }

    // Getters / Setters
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public List<String> getColumns() { return columns; }
    public List<Map<String, Object>> getRows() { return rows; }
    public Map<String, Object> getNewRow() { return newRow; }
    public Map<String, Object> getEditRow() { return editRow; }
    public Long getEditRowId() { return editRowId; }
    public boolean isEditing() { return editRow != null; }
    public Map<String, String> getFkColumns() { return fkColumns; }
    public Map<String, List<Map<String, Object>>> getFkOptions() { return fkOptions; }
}
