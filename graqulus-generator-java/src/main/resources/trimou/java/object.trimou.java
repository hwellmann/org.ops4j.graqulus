package {{packageName}};

{{#if listRequired}}
import java.util.List;
{{/if}}
import javax.annotation.Generated;
{{#if notNullRequired}}
import javax.validation.constraints.NotNull;
{{/if}}

{{>generated}}
public class {{typeName}} {{>implements}} {

{{#fieldModels}}
    private {{&typeName}} {{fieldName}};    
{{/fieldModels}}
    
{{#fieldModels}}
{{>objectFieldAccessors}}    
{{/fieldModels}}

}