package {{packageName}};

import javax.annotation.Generated;

{{>generated}}
public enum {{typeName}} {

{{#valueNames}}
    {{this}}{{#iter.hasNext}},{{/iter.hasNext}}{{#iter.isLast}};{{/iter.isLast}}
{{/valueNames}}
}
