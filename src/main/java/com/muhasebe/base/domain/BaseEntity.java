package com.muhasebe.base.domain;

import java.io.Serializable;


public interface BaseEntity extends Serializable {

    Long getId();

    void setId(Long id);
}
