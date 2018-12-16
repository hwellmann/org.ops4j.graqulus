package org.ops4j.graqulus.generator.trimou;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trimou.Mustache;
import org.trimou.engine.MustacheEngine;
import org.trimou.engine.MustacheEngineBuilder;
import org.trimou.engine.config.EngineConfigurationKey;
import org.trimou.engine.locator.ClassPathTemplateLocator;
import org.trimou.engine.locator.FileSystemTemplateLocator;
import org.trimou.handlebars.HelpersBuilder;
import org.trimou.util.ImmutableMap;

import graphql.schema.idl.TypeDefinitionRegistry;

public class TemplateEngine {

    public static final String TEMPLATE_SUFFIX = "trimou.java";

    public static final String TEMPLATE_PATH = "trimou/java";

    private static final int PRIO_CLASS_PATH = 100;
    private static final int PRIO_FILE_SYSTEM = 200;

    private static Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private MustacheEngine engine;

    private String templateDir;

    /**
     * Renders a template of the given name with the given action, using the parameter name
     * {@code action}.
     *
     * @param templateName
     *            template name
     * @param api
     *            RAML API model as context object
     * @return rendered template
     */
    public String renderTemplate(String templateName, TypeDefinitionRegistry schema) {
        Mustache mustache = getEngine().getMustache(templateName);
        String result = mustache.render(ImmutableMap.of("schema", schema));
        log.debug(result);
        return result;
    }

    public String renderTemplate(String templateName, Object model) {
        Mustache mustache = getEngine().getMustache(templateName);
        String result = mustache.render(model);
        log.debug(result);
        return result;
    }

    /**
     * Gets the template directory.
     *
     * @return template directory
     */
    public String getTemplateDir() {
        return templateDir;
    }

    /**
     * Set the template directory.
     *
     * @param templateDir
     *            template directory
     */
    public void setTemplateDir(String templateDir) {
        this.templateDir = templateDir;
    }

    /**
     * Constructs a template engine with some additional helpers and lambdas for HTML generation.
     */
    public MustacheEngine getEngine() {
        if (engine == null) {
            ClassPathTemplateLocator genericLocator = new ClassPathTemplateLocator(PRIO_CLASS_PATH,
                TEMPLATE_PATH, TEMPLATE_SUFFIX);
            MustacheEngineBuilder builder = MustacheEngineBuilder.newBuilder()
                .setProperty(EngineConfigurationKey.DEFAULT_FILE_ENCODING,
                    StandardCharsets.UTF_8.name())
                .addTemplateLocator(genericLocator)
                .registerHelpers(HelpersBuilder.builtin().addInclude().addInvoke().addSet()
                    .addSwitch().addWith().build());
            if (templateDir != null) {
                builder.addTemplateLocator(
                    new FileSystemTemplateLocator(PRIO_FILE_SYSTEM, templateDir, TEMPLATE_SUFFIX));
            }
            engine = builder.build();
        }
        return engine;
    }
}
