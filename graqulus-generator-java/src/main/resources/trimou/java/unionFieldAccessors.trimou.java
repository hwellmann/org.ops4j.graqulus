    public {{&typeName}} {{getterName}}() {
        if (value instanceof {{&typeName}}) {
			return ({{&typeName}}) value;
		}
        return null;
    }


    public void {{setterName}}({{&typeName}} {{fieldName}}) {
        this.value = {{fieldName}};
    }

