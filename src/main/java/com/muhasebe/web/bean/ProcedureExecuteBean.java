package com.muhasebe.web.bean;

import com.muhasebe.dynamic.dml.DynamicTableService;
import com.muhasebe.dynamic.procedure.DynamicProcedureService;
import com.muhasebe.tablemap.domain.DynamicProcedureEntity;
import com.muhasebe.web.util.FacesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.*;

@Named
@ViewScoped
public class ProcedureExecuteBean implements Serializable {

    @Inject
    private DynamicProcedureService procedureService;

    @Inject
    private DynamicTableService dmlService;

    private String procedureName;
    private DynamicProcedureEntity procedureMeta;
    private List<String> affectedTables = new ArrayList<>();
    private Map<String, List<Map<String, Object>>> tableData = new HashMap<>();
    private Map<String, List<String>> tableColumns = new HashMap<>();
    private Map<String, Long> selectedRows = new LinkedHashMap<>();
    private Map<String, String> inputValues = new LinkedHashMap<>();
    private List<DynamicProcedureEntity> allProcedures = new ArrayList<>();

    // HATAYI COZEN DEGISKENLER: Tablo ve Input alanlarını ayırdık
    private String tableNameToLoad;
    private String inputNameToAdd;

    @PostConstruct
    public void init() {
        try {
            allProcedures = procedureService.listAll();
        } catch (Exception e) { /* sessiz */ }

        String p = FacesUtil.getRequestParam("name");
        if (p != null && !p.isBlank()) {
            procedureName = p;
            loadProcedure();
        }
    }

    public void loadProcedure() {
        try {
            procedureMeta = procedureService.findByName(procedureName).orElse(null);
            if (procedureMeta == null) {
                FacesUtil.error("Islem bulunamadi: " + procedureName);
                return;
            }
            inputValues.clear();
            selectedRows.clear();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void selectTable(String table) {
        if (table == null || table.trim().isEmpty()) {
            FacesUtil.warn("Tablo adi bos olamaz!");
            return;
        }

        String cleanTable = table.trim().toLowerCase();
        if (!tableData.containsKey(cleanTable)) {
            try {
                tableColumns.put(cleanTable, dmlService.getTableColumns(cleanTable));
                tableData.put(cleanTable, dmlService.getTableData(cleanTable));
                if (!affectedTables.contains(cleanTable)) {
                    affectedTables.add(cleanTable);
                }
                this.tableNameToLoad = null; // Kutuyu temizle
            } catch (Exception e) {
                FacesUtil.error("Tablo yuklenemedi: " + cleanTable);
            }
        }
    }

    public void pickRow(String table, Object rowId) {
        if (rowId == null) return;
        Long id = rowId instanceof Number ? ((Number) rowId).longValue() : Long.parseLong(rowId.toString());
        selectedRows.put(table, id);
    }

    public void removeTablePick(String table) {
        selectedRows.remove(table);
        affectedTables.remove(table);
        tableData.remove(table);
        tableColumns.remove(table);
    }

    public void addInput(String name) {
        if (name != null && !name.isBlank() && !inputValues.containsKey(name)) {
            inputValues.put(name.trim(), "");
        }
    }

    public void addInputFromForm() {
        if (inputNameToAdd != null && !inputNameToAdd.trim().isEmpty()) {
            addInput(inputNameToAdd.trim());
            inputNameToAdd = null; // Kutuyu temizle
        }
    }

    public void removeInput(String name) {
        inputValues.remove(name);
    }

    public void execute() {
        if (procedureName == null || procedureName.isBlank()) {
            FacesUtil.warn("Procedure adi yok");
            return;
        }
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            List<Map<String, Object>> inputList = new ArrayList<>();
            for (Map.Entry<String, String> e : inputValues.entrySet()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", e.getKey());
                m.put("value", e.getValue());
                inputList.add(m);
            }
            params.put("inputValues", inputList);

            List<Map<String, Object>> idList = new ArrayList<>();
            for (Map.Entry<String, Long> e : selectedRows.entrySet()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("table", e.getKey());
                m.put("id", e.getValue());
                idList.add(m);
            }
            params.put("selectedIds", idList);

            procedureService.executeProcedure(procedureName, params);
            FacesUtil.info("Islem basariyla calistirildi!");
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public boolean isRowSelected(String table, Map<String, Object> row) {
        Long selected = selectedRows.get(table);
        if (selected == null) return false;
        Object id = row.get("id");
        if (id == null) return false;
        return selected.equals(((Number) id).longValue());
    }

    public void deleteProcedure(String name) {
        try {
            procedureService.deleteProcedure(name);
            FacesUtil.info("Silindi: " + name);
            allProcedures = procedureService.listAll();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    // GETTERS & SETTERS
    public String getProcedureName() { return procedureName; }
    public void setProcedureName(String procedureName) { this.procedureName = procedureName; }
    public DynamicProcedureEntity getProcedureMeta() { return procedureMeta; }
    public List<String> getAffectedTables() { return affectedTables; }
    public Map<String, List<Map<String, Object>>> getTableData() { return tableData; }
    public Map<String, List<String>> getTableColumns() { return tableColumns; }
    public Map<String, Long> getSelectedRows() { return selectedRows; }
    public Map<String, String> getInputValues() { return inputValues; }
    public List<DynamicProcedureEntity> getAllProcedures() { return allProcedures; }

    public String getTableNameToLoad() { return tableNameToLoad; }
    public void setTableNameToLoad(String tableNameToLoad) { this.tableNameToLoad = tableNameToLoad; }
    public String getInputNameToAdd() { return inputNameToAdd; }
    public void setInputNameToAdd(String inputNameToAdd) { this.inputNameToAdd = inputNameToAdd; }
}