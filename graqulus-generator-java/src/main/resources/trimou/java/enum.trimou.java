package {{packageName}};

import javax.annotation.Generated;

{{>description}}
{{>generated}}
public enum {{typeName}} {
{{#valueModels}}

    {{>enumValueDescription}}
    {{name}}{{#iter.hasNext}},{{/iter.hasNext}}{{#iter.isLast}};{{/iter.isLast}}
{{/valueModels}}
}
