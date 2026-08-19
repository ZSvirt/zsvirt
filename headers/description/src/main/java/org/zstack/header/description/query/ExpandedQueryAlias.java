package org.zstack.header.description.query;

/**
 * Public short name for an expanded-query path (e.g. {@code loadBalancerListener}
 * → {@code loadBalancerListenerRef.listener}).
 */
public final class ExpandedQueryAlias {
    public final Class<?> inventoryClass;
    public final String alias;
    public final String expandedField;

    public ExpandedQueryAlias(Class<?> inventoryClass, String alias, String expandedField) {
        this.inventoryClass = inventoryClass;
        this.alias = alias;
        this.expandedField = expandedField;
    }
}
