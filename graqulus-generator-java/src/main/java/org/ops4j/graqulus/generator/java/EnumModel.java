package org.ops4j.graqulus.generator.java;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.ZonedDateTime;
import java.util.List;

public class EnumModel {

    private String packageName;
    private String typeName;
    private List<EnumValueModel> valueModels;
    private String date;
    private String description;

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

    public List<EnumValueModel> getValueModels() {
        return valueModels;
    }

    public void setValueModels(List<EnumValueModel> valueModels) {
        this.valueModels = valueModels;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
