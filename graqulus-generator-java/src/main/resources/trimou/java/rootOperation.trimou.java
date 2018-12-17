package {{packageName}};

{{#if listRequired}}
import java.util.List;
{{/if}}
{{#if notNullRequired}}
import javax.validation.constraints.NotNull;
{{/if}}
import javax.annotation.Generated;

{{>generated}}
public interface {{typeName}} {

{{#fieldModels}}
{{>method}}    
{{/fieldModels}}

}
