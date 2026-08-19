package org.zstack.header.description.query;

import org.zstack.utils.DebugUtils;

/**
 * One hop in an {@link ExpandedQueryBuilder} chain. Call {@link #end()} to return to the parent builder.
 */
public class ExpandedQueryStepBuilder {
    final ExpandedQueryBuilder parent;
    final Class<?> hopInventoryClass;
    String named;
    Class<?> whereSourceClass;
    String foreignKey;
    String expandedInventoryKey;
    Class<?> suppressedInventoryClass;

    ExpandedQueryStepBuilder(ExpandedQueryBuilder parent, Class<?> hopInventoryClass) {
        this.parent = parent;
        this.hopInventoryClass = hopInventoryClass;
    }

    public ExpandedQueryStepBuilder named(String fieldName) {
        DebugUtils.Assert(fieldName != null && !fieldName.isEmpty(), "named field cannot be empty");
        this.named = fieldName;
        return this;
    }

    /**
     * Source side of the join: {@code inventoryClassToExpand}.{@code key}.
     */
    public ExpandedQueryStepBuilder where(Class<?> sourceInventoryClass, String key) {
        DebugUtils.Assert(sourceInventoryClass != null, "where inventory class cannot be null");
        DebugUtils.Assert(key != null && !key.isEmpty(), "where key cannot be empty");
        this.whereSourceClass = sourceInventoryClass;
        this.foreignKey = key;
        return this;
    }

    /**
     * Target side of the join: this hop inventory.{@code key}.
     */
    public ExpandedQueryStepBuilder equals(Class<?> targetInventoryClass, String key) {
        DebugUtils.Assert(targetInventoryClass != null, "equals inventory class cannot be null");
        DebugUtils.Assert(key != null && !key.isEmpty(), "equals key cannot be empty");
        DebugUtils.Assert(foreignKey != null, "call where(Class, key) before equals()");
        DebugUtils.Assert(targetInventoryClass.equals(hopInventoryClass),
                "equals inventory class must be the then(...) hop class");
        this.expandedInventoryKey = key;
        return this;
    }

    public ExpandedQueryStepBuilder suppress(Class<?> suppressedInventoryClass) {
        DebugUtils.Assert(suppressedInventoryClass != null, "suppressed inventory class cannot be null");
        this.suppressedInventoryClass = suppressedInventoryClass;
        return this;
    }

    /**
     * Finish this hop and return to {@link ExpandedQueryBuilder}.
     */
    public ExpandedQueryBuilder end() {
        if (foreignKey != null) {
            DebugUtils.Assert(expandedInventoryKey != null, "call equals(Class, key) after where()");
        }
        parent.ensureStepRegistered(this);
        return parent;
    }
}
