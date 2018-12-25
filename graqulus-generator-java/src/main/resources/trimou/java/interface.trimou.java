package {{packageName}};

{{>imports}}

{{>description}}
// Interface
{{>generated}}
public interface {{typeName}} {

{{#fieldModels}}
{{>interfaceFieldAccessors}}    
{{/fieldModels}}
}
