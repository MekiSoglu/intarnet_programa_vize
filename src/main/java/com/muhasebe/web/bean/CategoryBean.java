package com.muhasebe.web.bean;

import com.muhasebe.category.domain.CategoryDetailsEntity;
import com.muhasebe.category.domain.CategoryEntity;
import com.muhasebe.category.service.CategoryDetailsService;
import com.muhasebe.category.service.CategoryService;
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
public class CategoryBean implements Serializable {

    @Inject
    private CategoryService categoryService;

    @Inject
    private CategoryDetailsService detailsService;

    private List<CategoryEntity> categories = new ArrayList<>();
    private List<CategoryEntity> rootCategories = new ArrayList<>();
    private List<CategoryDetailsEntity> allDetails = new ArrayList<>();
    private CategoryEntity selected = new CategoryEntity();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        try {
            categories = categoryService.findAll();
            rootCategories = categoryService.findRoots();
            allDetails = detailsService.findAll();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void newCategory() {
        selected = new CategoryEntity();
    }

    public void save() {
        try {
            if (selected.getId() == null) {
                categoryService.create(selected);
                FacesUtil.info("Kategori eklendi");
            } else {
                categoryService.update(selected);
                FacesUtil.info("Kategori guncellendi");
            }
            selected = new CategoryEntity();
            refresh();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void edit(CategoryEntity c) {
        selected = c;
    }

    public void delete(Long id) {
        try {
            CategoryService.DeletionSummary summary = categoryService.deleteWithCascade(id);
            String msg = "Silme tamamlandi: " + summary.categoryCount + " kategori, "
                    + summary.tableCount + " tablo silindi";
            FacesUtil.info(msg);
            refresh();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public String getDeletePreview(Long id) {
        if (id == null) return "";
        try {
            CategoryService.DeletionSummary p = categoryService.previewDelete(id);
            if (p.tableCount == 0 && p.categoryCount == 1) {
                return "Bu kategori silinecek.";
            }
            return "DIKKAT: " + p.categoryCount + " kategori ve " + p.tableCount
                    + " tablo (icindeki tum verilerle) silinecek!";
        } catch (Exception e) {
            return "Silinecek.";
        }
    }

    public List<CategoryEntity> getChildrenOf(Long parentId) {
        List<CategoryEntity> children = new ArrayList<>();
        for (CategoryEntity c : categories) {
            if (parentId != null && parentId.equals(c.getParentId())) {
                children.add(c);
            }
        }
        return children;
    }

    public String getCategoryPath(CategoryEntity c) {
        if (c == null) return "";
        if (c.getParentId() == null) return c.getName();
        for (CategoryEntity parent : categories) {
            if (parent.getId().equals(c.getParentId())) {
                return getCategoryPath(parent) + " > " + c.getName();
            }
        }
        return c.getName();
    }

    public List<CategoryEntity> getCategories() { return categories; }
    public List<CategoryEntity> getRootCategories() { return rootCategories; }
    public List<CategoryDetailsEntity> getAllDetails() { return allDetails; }
    public CategoryEntity getSelected() { return selected; }
    public void setSelected(CategoryEntity selected) { this.selected = selected; }
}
