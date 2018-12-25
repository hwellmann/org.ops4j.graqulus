package org.ops4j.graqulus.starwars;

import javax.enterprise.context.Dependent;

import org.ops4j.graqulus.cdi.api.Resolver;

@Dependent
public class HumanResolver implements Resolver<Human> {

    @Override
    public boolean loadAllById() {
        return true;
    }
}
