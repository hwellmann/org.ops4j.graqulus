package org.ops4j.graqulus.starwars;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.enterprise.context.ApplicationScoped;

import org.ops4j.graqulus.cdi.api.Serializer;

@ApplicationScoped
public class LocalDateSerializer implements Serializer<LocalDate, String> {

    @Override
    public String serializeNonNull(LocalDate object) {
        return object.toString();
    }

    @Override
    public LocalDate deserializeNonNull(String serial) {
        return LocalDate.parse(serial, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
