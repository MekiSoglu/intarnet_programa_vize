package com.muhasebe.category.service;

import com.muhasebe.base.service.BaseService;
import com.muhasebe.category.domain.CategoryEntity;
import com.muhasebe.category.repository.CategoryRepository;
import com.muhasebe.dynamic.ddl.DynamicCreateTableService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@Stateless
public class CategoryService extends BaseService<CategoryEntity, CategoryRepository> {

    @Inject
    private CategoryRepository repository;

    @Inject
    private DynamicCreateTableService ddlService;

    @Override
    protected CategoryRepository getRepository() {
        return repository;
    }

    public List<CategoryEntity> findRoots() {
        return repository.findRoots();
    }

    public List<CategoryEntity> findChildren(Long parentId) {
        return repository.findByParentId(parentId);
    }

    public DeletionSummary deleteWithCascade(Long categoryId) {
        DeletionSummary summary = new DeletionSummary();
        List<Long> allIds = collectDescendantIds(categoryId);
        summary.categoryCount = allIds.size();

        for (Long id : allIds) {
            int dropped = ddlService.dropAllTablesOfCategory(id);
            summary.tableCount += dropped;
        }

        delete(categoryId);
        return summary;
    }

    private List<Long> collectDescendantIds(Long rootId) {
        List<Long> result = new ArrayList<>();
        result.add(rootId);
        for (CategoryEntity child : repository.findByParentId(rootId)) {
            result.addAll(collectDescendantIds(child.getId()));
        }
        return result;
    }

    public DeletionSummary previewDelete(Long categoryId) {
        DeletionSummary summary = new DeletionSummary();
        List<Long> allIds = collectDescendantIds(categoryId);
        summary.categoryCount = allIds.size();
        for (Long id : allIds) {
            summary.tableCount += ddlService.listTablesByCategory(id).size();
        }
        return summary;
    }

    public static class DeletionSummary {
        public int categoryCount;
        public int tableCount;
    }
}