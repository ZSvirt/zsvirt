package org.zstack.header.vo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface EntityGraph {
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Neighbour {
        Class type();
        String myField();
        String targetField();
        int weight() default -1;
    }

    Neighbour[] parents() default {};
    Neighbour[] friends() default {};
}
