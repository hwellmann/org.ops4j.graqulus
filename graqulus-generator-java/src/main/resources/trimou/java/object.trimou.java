package {{packageName}};

{{>imports}}

// Object
{{>generated}}
public class {{typeName}} {{>implements}} {

{{#fieldModels}}
    private {{&type.name}} {{fieldName}};    
{{/fieldModels}}
    
{{#fieldModels}}
{{>objectFieldAccessors}}    
{{/fieldModels}}
}