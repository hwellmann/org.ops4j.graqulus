package org.ops4j.graqulus.generator.java;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.ZonedDateTime;
import java.util.List;

public class EnumModel {

    private String packageName;
    private String typeName;
    private List<String> valueNames;
    private String date;

    public EnumModel() {
        this.date = ZonedDateTime.now().truncatedTo(SECONDS).format(ISO_OFFSET_DATE_TIME);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public List<String> getValueNames() {
        return valueNames;
    }

    public void setValueNames(List<String> valueNames) {
        this.valueNames = valueNames;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
