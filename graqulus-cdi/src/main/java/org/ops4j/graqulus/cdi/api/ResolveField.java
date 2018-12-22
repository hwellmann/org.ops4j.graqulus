package org.ops4j.graqulus.cdi.api;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface ResolveField {
    String value() default "";
    boolean caching() default true;
    boolean batching() default true;
}
