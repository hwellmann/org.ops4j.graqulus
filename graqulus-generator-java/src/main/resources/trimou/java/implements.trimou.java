{{#if interfaces}}implements {{#interfaces}}{{this}}{{#iter.hasNext}}, {{/iter.hasNext}}{{/interfaces}}{{/if}}