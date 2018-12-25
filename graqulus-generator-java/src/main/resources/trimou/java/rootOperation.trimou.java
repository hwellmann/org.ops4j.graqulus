package {{packageName}};

{{>imports}}

// Root Operation
{{>generated}}
public interface {{typeName}} {

{{#fieldModels}}
{{>method}}    
{{/fieldModels}}
}
