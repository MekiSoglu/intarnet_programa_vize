package com.muhasebe.web.bean;

import com.muhasebe.category.service.CategoryService;
import com.muhasebe.dynamic.ddl.DynamicCreateTableService;
import com.muhasebe.dynamic.procedure.DynamicProcedureService;
import com.muhasebe.tablemap.service.ViewMapService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@ViewScoped
public class HomeBean implements Serializable {

    @Inject private CategoryService categoryService;
    @Inject private DynamicCreateTableService ddlService;
    @Inject private ViewMapService viewMapService;
    @Inject private DynamicProcedureService procedureService;

    private long categoryCount;
    private long tableCount;
    private long viewCount;
    private long procedureCount;

    @PostConstruct
    public void init() {
        try {
            categoryCount = categoryService.count();
            tableCount = ddlService.listDynamicTables().size();
            viewCount = viewMapService.count();
            procedureCount = procedureService.listAll().size();
        } catch (Exception e) {}
    }

    public long getCategoryCount() { return categoryCount; }
    public long getTableCount() { return tableCount; }
    public long getViewCount() { return viewCount; }
    public long getProcedureCount() { return procedureCount; }
}