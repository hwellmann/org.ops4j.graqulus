package {{packageName}};

import javax.annotation.Generated;

{{>generated}}
public enum {{typeName}} {

{{#valueNames}}
    {{this}},
{{/valueNames}}
    // trailing comma is intentional
    ;
}
