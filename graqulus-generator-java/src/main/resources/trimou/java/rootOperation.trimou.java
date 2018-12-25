package {{packageName}};

{{>imports}}

{{>description}}
// Root Operation
{{>generated}}
public interface {{typeName}} {

{{#fieldModels}}
{{>method}}    
{{/fieldModels}}
}
