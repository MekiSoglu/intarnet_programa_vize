package com.muhasebe.web.bean;

import com.muhasebe.category.domain.CategoryEntity;
import com.muhasebe.category.service.CategoryService;
import com.muhasebe.dynamic.ddl.DynamicCreateTableService;
import com.muhasebe.web.util.FacesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;


import java.util.*;

@Named
@ViewScoped
public class TableCreateBean implements Serializable {

    @Inject
    private DynamicCreateTableService ddlService;

    @Inject
    private CategoryService categoryService;

    private String tableName;
    private String tableToDelete;
    private Long selectedCategoryId;
    private List<ColumnRow> columns = new ArrayList<>();
    private List<ForeignKeyRow> foreignKeys = new ArrayList<>();
    private List<String> existingTables = new ArrayList<>();
    private List<CategoryEntity> allCategories = new ArrayList<>();

    @PostConstruct
    public void init() {
        addColumn();
        refreshExistingTables();
        refreshCategories();
    }

    public void refreshExistingTables() {
        try {
            existingTables = ddlService.listAllTables();
        } catch (Exception e) {
            existingTables = new ArrayList<>();
        }
    }

    public void refreshCategories() {
        try {
            allCategories = categoryService.findAll();
        } catch (Exception e) {
            allCategories = new ArrayList<>();
        }
    }

    public String getCategoryPath(CategoryEntity c) {
        if (c == null) return "";
        if (c.getParentId() == null) return c.getName();
        for (CategoryEntity parent : allCategories) {
            if (parent.getId().equals(c.getParentId())) {
                return getCategoryPath(parent) + " > " + c.getName();
            }
        }
        return c.getName();
    }

    public void addColumn() { columns.add(new ColumnRow()); }
    public void removeColumn(ColumnRow row) { columns.remove(row); }

    public void addForeignKey() { foreignKeys.add(new ForeignKeyRow()); }
    public void removeForeignKey(ForeignKeyRow row) { foreignKeys.remove(row); }

    public String createTable() {
        try {
            List<Map<String, String>> colMaps = new ArrayList<>();
            for (ColumnRow c : columns) {
                if (c.getKey() == null || c.getKey().isBlank()) continue;
                Map<String, String> m = new HashMap<>();
                m.put("key", c.getKey());
                m.put("type", c.getType());
                if (c.isEnum()) {
                    m.put("enumValues", c.getEnumValues());
                    m.put("defaultValue", c.getDefaultValue());
                }
                colMaps.add(m);
            }

            List<Map<String, String>> fkMaps = new ArrayList<>();
            for (ForeignKeyRow fk : foreignKeys) {
                if (fk.getReferences() == null || fk.getReferences().isBlank()) continue;
                Map<String, String> m = new HashMap<>();
                m.put("relation", fk.getRelation());
                m.put("references", fk.getReferences());
                m.put("column", fk.getColumn());
                m.put("relationColumn", fk.getRelationColumn());
                fkMaps.add(m);
            }

            ddlService.createTable(tableName, colMaps, fkMaps, false, selectedCategoryId);
            FacesUtil.info("Tablo basariyla olusturuldu: " + tableName);
            return FacesUtil.redirect("/pages/tables/list.xhtml");
        } catch (Exception e) {
            FacesUtil.error(e);
            return null;
        }
    }

    public void deleteSelectedTable() {
        if (tableToDelete == null || tableToDelete.isBlank()) {
            FacesUtil.warn("Silinecek tablo secilmedi");
            return;
        }
        try {
            String result = ddlService.dropTable(tableToDelete);
            FacesUtil.info(result);
            tableToDelete = null;
            refreshExistingTables();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public String[] getAvailableTypes() {
        return new String[]{"varchar", "integer", "decimal", "date", "boolean", "text", "enum"};
    }

    public String[] getRelationTypes() {
        return new String[]{"one-to-many", "many-to-many"};
    }

    // ============================================================
//  FK icin hedef tablonun kolonlarini saglayan yardimcilar
// ============================================================

    /** Hedef tablo -> kolon listesi (her turda DB'ye gitmemek icin cache). */
    private Map<String, List<String>> fkTargetColumnsCache = new HashMap<>();

    /**
     * AJAX listener: FK satirinda hedef tablo degisince kolonlari DB'den yukler.
     * Ayrica FK kolon adi bos ise otomatik "<hedef>_id" onerisi yapilir.
     */
    public void onFkTargetChange(ForeignKeyRow row) {
        if (row.getReferences() == null || row.getReferences().isBlank()) {
            return;
        }
        loadFkTargetColumns(row.getReferences());
        // Kullanici henuz FK kolon adi girmediyse otomatik oner
        if (row.getColumn() == null || row.getColumn().isBlank()) {
            row.setColumn(row.getReferences() + "_id");
        }
        // Display kolonu bos ise ilk "anlamli" kolonu oner (id/created_date disinda)
        if (row.getRelationColumn() == null || row.getRelationColumn().isBlank()) {
            for (String col : fkTargetColumnsCache.getOrDefault(row.getReferences(), Collections.emptyList())) {
                if (!"id".equals(col) && !"created_date".equals(col)) {
                    row.setRelationColumn(col);
                    break;
                }
            }
        }
    }

    private void loadFkTargetColumns(String tableName) {
        if (fkTargetColumnsCache.containsKey(tableName)) return;
        try {
            fkTargetColumnsCache.put(tableName, ddlService.getTableColumns(tableName));
        } catch (Exception e) {
            fkTargetColumnsCache.put(tableName, Collections.emptyList());
        }
    }

    /** XHTML'den cagirilacak: bu FK satiri icin display kolonu dropdown secenekleri. */
    public List<String> getColumnsForFk(ForeignKeyRow row) {
        if (row.getReferences() == null || row.getReferences().isBlank()) {
            return Collections.emptyList();
        }
        loadFkTargetColumns(row.getReferences());
        return fkTargetColumnsCache.getOrDefault(row.getReferences(), Collections.emptyList());
    }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getTableToDelete() { return tableToDelete; }
    public void setTableToDelete(String tableToDelete) { this.tableToDelete = tableToDelete; }

    public List<ColumnRow> getColumns() { return columns; }
    public void setColumns(List<ColumnRow> columns) { this.columns = columns; }

    public List<ForeignKeyRow> getForeignKeys() { return foreignKeys; }
    public void setForeignKeys(List<ForeignKeyRow> foreignKeys) { this.foreignKeys = foreignKeys; }

    public List<String> getExistingTables() { return existingTables; }

    public Long getSelectedCategoryId() { return selectedCategoryId; }
    public void setSelectedCategoryId(Long selectedCategoryId) { this.selectedCategoryId = selectedCategoryId; }

    public List<CategoryEntity> getAllCategories() { return allCategories; }

    public static class ColumnRow implements Serializable {
        private String key;
        private String type = "varchar";
        private String enumValues;
        private String defaultValue;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getEnumValues() { return enumValues; }
        public void setEnumValues(String enumValues) { this.enumValues = enumValues; }

        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

        public boolean isEnum() { return "enum".equalsIgnoreCase(type); }
    }

    public static class ForeignKeyRow implements Serializable {
        private String relation = "one-to-many";
        private String references;
        private String column;
        private String relationColumn;
        public String getRelation() { return relation; }
        public void setRelation(String relation) { this.relation = relation; }
        public String getReferences() { return references; }
        public void setReferences(String references) { this.references = references; }
        public String getColumn() { return column; }
        public void setColumn(String column) { this.column = column; }
        public String getRelationColumn() { return relationColumn; }
        public void setRelationColumn(String relationColumn) { this.relationColumn = relationColumn; }
    }
}