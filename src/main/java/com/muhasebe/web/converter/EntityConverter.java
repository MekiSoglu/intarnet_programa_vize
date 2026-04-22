package com.muhasebe.web.converter;

import com.muhasebe.base.domain.BaseEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * JSF selectOneMenu/selectManyMenu icin generic entity converter.
 * Entity'leri ID'leri ile serialize/deserialize eder.
 *
 * Kullanim: <p:selectOneMenu converter="entityConverter">
 *
 * Her render cycle'inda view'deki entity'leri UI cache'ine yaziyor,
 * sonra form submit'te ID'den entity'ye donuyor.
 */
@FacesConverter(value = "entityConverter", managed = true)
@ApplicationScoped
public class EntityConverter implements Converter<BaseEntity> {

    @Override
    public String getAsString(FacesContext ctx, UIComponent comp, BaseEntity entity) {
        if (entity == null || entity.getId() == null) return "";

        // Component attribute olarak entity'yi cache'le (component'e ozgu)
        Map<String, Object> attrs = comp.getAttributes();
        @SuppressWarnings("unchecked")
        Map<String, BaseEntity> cache = (Map<String, BaseEntity>) attrs.get("entityConverterCache");
        if (cache == null) {
            cache = new HashMap<>();
            attrs.put("entityConverterCache", cache);
        }
        String key = entity.getClass().getSimpleName() + ":" + entity.getId();
        cache.put(key, entity);
        return key;
    }

    @Override
    public BaseEntity getAsObject(FacesContext ctx, UIComponent comp, String value) {
        if (value == null || value.isBlank()) return null;

        Map<String, Object> attrs = comp.getAttributes();
        @SuppressWarnings("unchecked")
        Map<String, BaseEntity> cache = (Map<String, BaseEntity>) attrs.get("entityConverterCache");
        if (cache != null && cache.containsKey(value)) {
            return cache.get(value);
        }
        return null;
    }
}
