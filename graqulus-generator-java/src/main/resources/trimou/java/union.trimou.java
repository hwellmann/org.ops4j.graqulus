package {{packageName}};

{{#if listRequired}}
import java.util.List;
{{/if}}
import javax.annotation.Generated;
{{#if notNullRequired}}
import javax.validation.constraints.NotNull;
{{/if}}

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