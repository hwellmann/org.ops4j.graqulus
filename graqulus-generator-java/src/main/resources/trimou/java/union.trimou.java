package {{packageName}};

{{>imports}}

{{>description}}
// Union
{{>generated}}
public class {{typeName}} {{>implements}} {

    private Object value;

    public Object value() {
        return value;
    }

    public String type() {
        return value.getClass().getSimpleName();
    }
    
{{#fieldModels}}
{{>unionFieldAccessors}}    
{{/fieldModels}}
}