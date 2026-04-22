package com.muhasebe.web.bean;

import com.muhasebe.dynamic.join.DynamicJoinViewService;
import com.muhasebe.web.util.FacesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.*;

/**
 * View verisi gosterim sayfasi (pages/views/data.xhtml?view=viewAdi).
 */
@Named
@ViewScoped
public class ViewDataBean implements Serializable {

    @Inject
    private DynamicJoinViewService viewService;

    private String viewName;
    private List<Map<String, Object>> rows = new ArrayList<>();
    private List<String> columns = new ArrayList<>();

    private String filterColumn;
    private String filterValue;

    @PostConstruct
    public void init() {
        viewName = FacesUtil.getRequestParam("view");
        if (viewName != null && !viewName.isBlank()) {
            loadData();
        }
    }

    public void loadData() {
        try {
            rows = viewService.fetchDataFromView(viewName);
            extractColumns();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void filter() {
        if (filterColumn == null || filterColumn.isBlank()) {
            loadData();
            return;
        }
        try {
            rows = viewService.fetchFilteredDataFromView(viewName, filterColumn, filterValue);
            extractColumns();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void clearFilter() {
        filterColumn = null;
        filterValue = null;
        loadData();
    }

    private void extractColumns() {
        if (!rows.isEmpty()) {
            columns = new ArrayList<>(rows.get(0).keySet());
        } else {
            columns = Collections.emptyList();
        }
    }

    public String getViewName() { return viewName; }
    public void setViewName(String viewName) { this.viewName = viewName; }

    public List<Map<String, Object>> getRows() { return rows; }
    public List<String> getColumns() { return columns; }

    public String getFilterColumn() { return filterColumn; }
    public void setFilterColumn(String filterColumn) { this.filterColumn = filterColumn; }

    public String getFilterValue() { return filterValue; }
    public void setFilterValue(String filterValue) { this.filterValue = filterValue; }
}
