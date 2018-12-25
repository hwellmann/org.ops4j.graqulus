{{>getterDescription}}
    {{>override}}
    public {{&type.name}} {{getterName}}() {
        return {{fieldName}};
    }

{{>setterDescription}}
    {{>override}}
    public void {{setterName}}({{&type.name}} {{fieldName}}) {
        this.{{fieldName}} = {{fieldName}};
    }

