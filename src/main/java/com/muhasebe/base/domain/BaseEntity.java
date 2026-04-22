package com.muhasebe.base.domain;

import java.io.Serializable;

/**
 * Tum entity'lerin uygulamasi gereken minimum interface.
 * (Spring Data tarafindan dayatilan generic ID pattern'inin Jakarta EE karsiligi.)
 */
public interface BaseEntity extends Serializable {

    Long getId();

    void setId(Long id);
}
