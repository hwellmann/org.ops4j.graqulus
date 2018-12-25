package org.ops4j.graqulus.starwars;

import javax.enterprise.context.Dependent;

import org.ops4j.graqulus.cdi.api.Resolver;

@Dependent
public class DroidResolver implements Resolver<Droid> {

    @Override
    public boolean loadAllById() {
        return true;
    }
}
