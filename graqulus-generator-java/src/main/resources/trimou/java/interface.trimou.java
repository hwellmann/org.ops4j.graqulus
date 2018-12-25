package {{packageName}};

{{>imports}}

// Interface
{{>generated}}
public interface {{typeName}} {

{{#fieldModels}}
{{>interfaceFieldAccessors}}    
{{/fieldModels}}
}
