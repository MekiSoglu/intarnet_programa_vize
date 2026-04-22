package com.muhasebe.web.bean;

import com.muhasebe.category.domain.CategoryEntity;
import com.muhasebe.category.service.CategoryService;
import com.muhasebe.dynamic.ddl.DynamicCreateTableService;
import com.muhasebe.dynamic.dml.DynamicTableService;
import com.muhasebe.web.util.FacesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.*;

@Named
@ViewScoped
public class AraclarBean implements Serializable {

    @Inject private CategoryService categoryService;
    @Inject private DynamicCreateTableService ddlService;
    @Inject private DynamicTableService dmlService;

    private List<CategoryEntity> rootCategories = new ArrayList<>();
    private Map<Long, List<CategoryEntity>> childrenByParent = new HashMap<>();
    private Map<Long, List<String>> tablesByCategory = new HashMap<>();
    private Set<Long> expandedCategories = new HashSet<>();

    private String openTable;
    private List<String> openTableColumns = new ArrayList<>();
    private List<Map<String, Object>> openTableRows = new ArrayList<>();
    private Map<String, Object> newRow = new LinkedHashMap<>();

    @PostConstruct
    public void init() { refresh(); }

    public void refresh() {
        try {
            List<CategoryEntity> all = categoryService.findAll();
            rootCategories = new ArrayList<>();
            childrenByParent = new HashMap<>();
            tablesByCategory = new HashMap<>();

            for (CategoryEntity c : all) {
                if (c.getParentId() == null) {
                    rootCategories.add(c);
                } else {
                    childrenByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
                }
                tablesByCategory.put(c.getId(), ddlService.listTablesByCategory(c.getId()));
            }
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public List<CategoryEntity> getChildren(Long parentId) {
        return childrenByParent.getOrDefault(parentId, Collections.emptyList());
    }

    public List<String> getTablesOf(Long categoryId) {
        return tablesByCategory.getOrDefault(categoryId, Collections.emptyList());
    }

    public boolean hasChildren(Long categoryId) {
        List<CategoryEntity> ch = childrenByParent.get(categoryId);
        return ch != null && !ch.isEmpty();
    }

    public boolean hasTables(Long categoryId) {
        List<String> t = tablesByCategory.get(categoryId);
        return t != null && !t.isEmpty();
    }

    public boolean isExpanded(Long categoryId) {
        return expandedCategories.contains(categoryId);
    }

    public void toggle(Long categoryId) {
        if (expandedCategories.contains(categoryId)) {
            expandedCategories.remove(categoryId);
        } else {
            expandedCategories.add(categoryId);
        }
    }

    public void toggleTable(String tableName) {
        if (tableName.equals(openTable)) {
            openTable = null;
            openTableColumns = new ArrayList<>();
            openTableRows = new ArrayList<>();
            newRow = new LinkedHashMap<>();
        } else {
            openTable = tableName;
            try {
                openTableColumns = dmlService.getTableColumns(tableName);
                openTableRows = dmlService.getTableData(tableName);
                resetNewRow();
            } catch (Exception e) {
                FacesUtil.error(e);
            }
        }
    }

    private void resetNewRow() {
        newRow = new LinkedHashMap<>();
        for (String col : openTableColumns) {
            if (!"id".equals(col) && !"created_date".equals(col)) {
                newRow.put(col, "");
            }
        }
    }

    public boolean isTableOpen(String tableName) {
        return tableName != null && tableName.equals(openTable);
    }

    public boolean isSystemCol(String col) {
        return "id".equals(col) || "created_date".equals(col);
    }

    public void addRow() {
        if (openTable == null) return;
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
            dmlService.insertIntoTable(openTable, filtered);
            FacesUtil.info("Kayit eklendi");
            openTableRows = dmlService.getTableData(openTable);
            resetNewRow();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void deleteRow(Map<String, Object> row) {
        if (openTable == null) return;
        try {
            Object idObj = row.get("id");
            if (idObj == null) return;
            dmlService.deleteFromTable(openTable, ((Number) idObj).longValue());
            openTableRows = dmlService.getTableData(openTable);
            FacesUtil.info("Kayit silindi");
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public List<CategoryEntity> getRootCategories() { return rootCategories; }
    public String getOpenTable() { return openTable; }
    public List<String> getOpenTableColumns() { return openTableColumns; }
    public List<Map<String, Object>> getOpenTableRows() { return openTableRows; }
    public Map<String, Object> getNewRow() { return newRow; }
}