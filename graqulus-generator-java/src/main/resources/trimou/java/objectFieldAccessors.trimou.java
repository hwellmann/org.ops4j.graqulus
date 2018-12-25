    {{>override}}
    public {{&type.name}} {{getterName}}() {
        return {{fieldName}};
    }

    {{>override}}
    public void {{setterName}}({{&type.name}} {{fieldName}}) {
        this.{{fieldName}} = {{fieldName}};
    }

