package org.zstack.header.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExpandedQuery {
    Class target() default Object.class;

    String expandedField();

    Class inventoryClass();

    String foreignKey();

    String expandedInventoryKey();

    boolean hidden() default false;
}
