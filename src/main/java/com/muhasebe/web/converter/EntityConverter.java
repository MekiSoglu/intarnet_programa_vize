package com.muhasebe.web.converter;

import com.muhasebe.base.domain.BaseEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import java.util.HashMap;
import java.util.Map;


@FacesConverter(value = "entityConverter", managed = true)
@ApplicationScoped
// java verilerini dönüştürerek uı a gönderiri cache yapısı var sürekli db ye sorgu atmaz
public class EntityConverter implements Converter<BaseEntity> {

    //nesneyi tarıyıcının anlayacağı formata çevir
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

    //tarayıcıdan gelen metni java ya çevir
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
