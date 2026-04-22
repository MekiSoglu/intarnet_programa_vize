package com.muhasebe.web.bean;

import com.muhasebe.category.domain.CategoryDetailsEntity;
import com.muhasebe.category.service.CategoryDetailsService;
import com.muhasebe.web.util.FacesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class CategoryDetailsBean implements Serializable {

    @Inject
    private CategoryDetailsService service;

    private List<CategoryDetailsEntity> items = new ArrayList<>();
    private CategoryDetailsEntity selected = new CategoryDetailsEntity();

    @PostConstruct
    public void init() { refresh(); }

    public void refresh() {
        try {
            items = service.findAll();
        } catch (Exception e) { FacesUtil.error(e); }
    }

    public void newItem() { selected = new CategoryDetailsEntity(); }

    public void save() {
        try {
            if (selected.getId() == null) {
                service.create(selected);
                FacesUtil.info("Detay eklendi");
            } else {
                service.update(selected);
                FacesUtil.info("Detay guncellendi");
            }
            selected = new CategoryDetailsEntity();
            refresh();
        } catch (Exception e) { FacesUtil.error(e); }
    }

    public void edit(CategoryDetailsEntity c) { selected = c; }

    public void delete(Long id) {
        try {
            service.delete(id);
            FacesUtil.info("Detay silindi");
            refresh();
        } catch (Exception e) { FacesUtil.error(e); }
    }

    public String[] getDataTypes() {
        return new String[]{"string", "number", "boolean", "date"};
    }

    public List<CategoryDetailsEntity> getItems() { return items; }
    public CategoryDetailsEntity getSelected() { return selected; }
    public void setSelected(CategoryDetailsEntity selected) { this.selected = selected; }
}
