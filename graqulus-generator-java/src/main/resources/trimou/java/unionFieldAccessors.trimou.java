    public {{&type.name}} {{getterName}}() {
        if (value instanceof {{&type.name}}) {
			return ({{&type.name}}) value;
		}
        return null;
    }

    public void {{setterName}}({{&type.name}} {{fieldName}}) {
        this.value = {{fieldName}};
    }

