package {{packageName}};

{{>imports}}

{{>generated}}
public interface {{typeName}} {

{{#fieldModels}}
{{>interfaceFieldAccessors}}    
{{/fieldModels}}

}
