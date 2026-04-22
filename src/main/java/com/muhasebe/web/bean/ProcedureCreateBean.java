package com.muhasebe.web.bean;

import com.muhasebe.dynamic.ddl.DynamicCreateTableService;
import com.muhasebe.dynamic.procedure.DynamicProcedureService;
import com.muhasebe.tablemap.domain.DynamicProcedureEntity;
import com.muhasebe.web.util.FacesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.*;

/**
 * Procedure (Islem) olusturma sayfasi.
 *
 * Angular'daki "islemler" sayfasinin karsiligi. Drag-drop yerine dropdown + buton:
 *   1. Islem adi gir
 *   2. Input'lar ekle (ad + tip)
 *   3. Operasyonlar ekle: tablo + kolon + operasyon + hangi input
 *   4. Olustur
 */
@Named
@ViewScoped
public class ProcedureCreateBean implements Serializable {

    @Inject
    private DynamicProcedureService procedureService;

    @Inject
    private DynamicCreateTableService ddlService;

    private String procedureName;
    private List<InputRow> inputs = new ArrayList<>();
    private List<OperationRow> operations = new ArrayList<>();

    private List<String> availableTables = new ArrayList<>();
    private List<DynamicProcedureEntity> existingProcedures = new ArrayList<>();

    /** Tablo -> Kolonlar cache'i (her drop'ta yuklemeyelim). */
    private Map<String, List<String>> columnsByTable = new HashMap<>();

    @PostConstruct
    public void init() {
        addInput();
        addOperation();
        refresh();
    }

    public void refresh() {
        try {
            availableTables = ddlService.listDynamicTables();
            existingProcedures = procedureService.listAll();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void addInput() { inputs.add(new InputRow()); }
    public void removeInput(InputRow r) { inputs.remove(r); }

    public void addOperation() { operations.add(new OperationRow()); }
    public void removeOperation(OperationRow r) { operations.remove(r); }

    /** AJAX listener: tablo secildiginde kolonlari yukle. */
    public void onTableChange(OperationRow row) {
        if (row.getTable() != null && !row.getTable().isBlank()) {
            loadColumnsFor(row.getTable());
        }
    }

    private void loadColumnsFor(String tableName) {
        if (columnsByTable.containsKey(tableName)) return;
        try {
            columnsByTable.put(tableName, ddlService.getTableColumns(tableName));
        } catch (Exception e) {
            columnsByTable.put(tableName, Collections.emptyList());
        }
    }

    public List<String> getColumnsForRow(OperationRow row) {
        if (row.getTable() == null || row.getTable().isBlank()) return Collections.emptyList();
        loadColumnsFor(row.getTable());
        return columnsByTable.getOrDefault(row.getTable(), Collections.emptyList());
    }

    public String[] getDataTypes() {
        return new String[]{"INTEGER", "BIGINT", "NUMERIC", "DECIMAL", "VARCHAR", "DATE", "BOOLEAN"};
    }

    public String[] getOperationTypes() {
        return new String[]{"increase", "decrease", "multiply", "divide", "set"};
    }

    public List<String> getInputNames() {
        List<String> names = new ArrayList<>();
        for (InputRow i : inputs) {
            if (i.getName() != null && !i.getName().isBlank()) names.add(i.getName());
        }
        return names;
    }

    public String createProcedure() {
        if (procedureName == null || procedureName.isBlank()) {
            FacesUtil.warn("Islem adi gerekli");
            return null;
        }
        try {
            List<Map<String, Object>> inputMaps = new ArrayList<>();
            for (InputRow i : inputs) {
                if (i.getName() == null || i.getName().isBlank()) continue;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", i.getName());
                m.put("dataType", i.getDataType());
                inputMaps.add(m);
            }

            List<Map<String, Object>> opMaps = new ArrayList<>();
            for (OperationRow o : operations) {
                if (o.getTable() == null || o.getColumn() == null) continue;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("table", o.getTable());
                m.put("column", o.getColumn());
                m.put("operation", o.getOperation());
                m.put("inputName", o.getInputName());
                opMaps.add(m);
            }

            if (opMaps.isEmpty()) {
                FacesUtil.warn("En az bir operasyon eklemelisiniz");
                return null;
            }

            Map<String, Object> jsonData = new LinkedHashMap<>();
            jsonData.put("inputs", inputMaps);
            jsonData.put("columns", opMaps);

            Map<String, Object> req = new LinkedHashMap<>();
            req.put("name", procedureName);
            req.put("jsonData", jsonData);

            procedureService.createProcedure(req);
            FacesUtil.info("Islem olusturuldu: " + procedureName);
            return FacesUtil.redirect("/pages/procedures/list.xhtml");
        } catch (Exception e) {
            FacesUtil.error(e);
            return null;
        }
    }

    public String getProcedureName() { return procedureName; }
    public void setProcedureName(String procedureName) { this.procedureName = procedureName; }
    public List<InputRow> getInputs() { return inputs; }
    public List<OperationRow> getOperations() { return operations; }
    public List<String> getAvailableTables() { return availableTables; }
    public List<DynamicProcedureEntity> getExistingProcedures() { return existingProcedures; }

    public static class InputRow implements Serializable {
        private String name;
        private String dataType = "NUMERIC";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
    }

    public static class OperationRow implements Serializable {
        private String table;
        private String column;
        private String operation = "increase";
        private String inputName;
        public String getTable() { return table; }
        public void setTable(String table) { this.table = table; }
        public String getColumn() { return column; }
        public void setColumn(String column) { this.column = column; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public String getInputName() { return inputName; }
        public void setInputName(String inputName) { this.inputName = inputName; }
    }
}
