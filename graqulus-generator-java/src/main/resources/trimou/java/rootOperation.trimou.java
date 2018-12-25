package {{packageName}};

{{>imports}}

{{>generated}}
public interface {{typeName}} {

{{#fieldModels}}
{{>method}}    
{{/fieldModels}}

}
