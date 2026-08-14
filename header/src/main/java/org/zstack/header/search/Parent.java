package org.zstack.header.search;

/**
 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Parent {
    Class<?> inventoryClass();

    String type();
}
