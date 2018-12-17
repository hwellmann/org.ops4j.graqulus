package org.ops4j.graqulus.cdi.impl;

import javax.enterprise.context.Dependent;

import org.ops4j.graqulus.cdi.api.IdPropertyStrategy;

@Dependent
public class DefaultIdPropertyStrategy implements IdPropertyStrategy {

    public static final String ID_DEFAULT = "id";

    @Override
    public String id(String typeName) {
        return ID_DEFAULT;
    }
}
