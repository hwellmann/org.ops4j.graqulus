package {{packageName}};

{{>imports}}

import javax.annotation.Generated;
{{#if notNullRequired}}
import javax.validation.constraints.NotNull;
{{/if}}

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