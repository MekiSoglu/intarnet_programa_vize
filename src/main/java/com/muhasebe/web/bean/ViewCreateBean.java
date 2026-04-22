package com.muhasebe.web.bean;

import com.muhasebe.dynamic.ddl.DynamicCreateTableService;
import com.muhasebe.dynamic.join.DynamicJoinViewService;
import com.muhasebe.tablemap.domain.ViewMap;
import com.muhasebe.web.util.FacesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cari (View) olusturma bean'i.
 *
 * Angular'daki "cari" component akisi:
 *   1. Kullanici cari adi girer
 *   2. Mevcut tablolardan birini secer
 *   3. Sistem iliski tablolarini gosterir
 *   4. Kullanici baska tablolar da secebilir
 *   5. "Cari Olustur" ile view olusur
 */
@Named
@ViewScoped
public class ViewCreateBean implements Serializable {

    @Inject
    private DynamicCreateTableService ddlService;

    @Inject
    private DynamicJoinViewService viewService;

    private String viewName;
    private List<String> availableTables = new ArrayList<>();
    private List<String> selectedTables = new ArrayList<>();
    private List<Map<String, Object>> relatedTables = new ArrayList<>();
    private List<ViewMap> existingViews = new ArrayList<>();
    private String tableToAdd;

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        try {
            availableTables = ddlService.listDynamicTables();
            existingViews = viewService.listAllViews();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    /** Bir tablo secildiginde, iliskili tablolari bul. */
    public void addTableToSelection() {
        if (tableToAdd == null || tableToAdd.isBlank()) return;
        if (!selectedTables.contains(tableToAdd)) {
            selectedTables.add(tableToAdd);
            refreshRelations();
        }
        tableToAdd = null;
    }

    public void removeSelectedTable(String t) {
        selectedTables.remove(t);
        refreshRelations();
    }

    private void refreshRelations() {
        try {
            if (selectedTables.isEmpty()) {
                relatedTables = new ArrayList<>();
            } else {
                relatedTables = viewService.getTableRelations(selectedTables);
            }
        } catch (Exception e) {
            relatedTables = new ArrayList<>();
        }
    }

    public void createView() {
        if (selectedTables.size() < 2) {
            FacesUtil.warn("En az 2 tablo secmelisiniz");
            return;
        }
        if (viewName == null || viewName.isBlank()) {
            FacesUtil.warn("Cari (view) adi giriniz");
            return;
        }
        try {
            String created = viewService.createDynamicView(selectedTables, relatedTables, viewName);
            FacesUtil.info("Cari olusturuldu: " + created);
            viewName = null;
            selectedTables.clear();
            relatedTables.clear();
            refresh();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void deleteView(String name) {
        try {
            viewService.deleteView(name);
            FacesUtil.info("Cari silindi: " + name);
            refresh();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public String getViewName() { return viewName; }
    public void setViewName(String viewName) { this.viewName = viewName; }

    public List<String> getAvailableTables() { return availableTables; }
    public List<String> getSelectedTables() { return selectedTables; }
    public void setSelectedTables(List<String> selectedTables) { this.selectedTables = selectedTables; }
    public List<Map<String, Object>> getRelatedTables() { return relatedTables; }
    public List<ViewMap> getExistingViews() { return existingViews; }

    public String getTableToAdd() { return tableToAdd; }
    public void setTableToAdd(String tableToAdd) { this.tableToAdd = tableToAdd; }
}
