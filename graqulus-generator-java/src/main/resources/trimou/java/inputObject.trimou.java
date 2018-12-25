package {{packageName}};

{{>imports}}

{{>description}}
// Input Object
{{>generated}}
public class {{typeName}} {{>implements}} {

{{#fieldModels}}
    private {{&type.name}} {{fieldName}};    
{{/fieldModels}}
    
{{#fieldModels}}
{{>objectFieldAccessors}}    
{{/fieldModels}}

}