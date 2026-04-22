package com.muhasebe.web.bean;

import com.muhasebe.dynamic.ddl.DynamicCreateTableService;
import com.muhasebe.web.util.FacesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tablo listesi sayfasi (pages/tables/list.xhtml) icin bean.
 * Tablolari listeler, secilen tabloyu silebilir.
 */
@Named
@ViewScoped
public class TableListBean implements Serializable {

    @Inject
    private DynamicCreateTableService ddlService;

    private List<String> tables = new ArrayList<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        try {
            tables = ddlService.listDynamicTables();
        } catch (Exception e) {
            FacesUtil.error(e);
            tables = new ArrayList<>();
        }
    }

    public void deleteTable(String tableName) {
        try {
            String result = ddlService.dropTable(tableName);
            FacesUtil.info(result);
            refresh();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public List<String> getTables() {
        return tables;
    }
}
