package org.zstack.header.description.query;

/**
 * One expanded-query hop: expand {@link #inventoryClassToExpand} with field {@link #expandedField}
 * joining to {@link #inventoryClass}.
 * <p>
 * Configuration only; query execution reads {@link org.zstack.header.description.PackageDescriptionRegistry}.
 */
public final class ExpandedQuery {
    public final Class<?> inventoryClassToExpand;
    public final String expandedField;
    public final Class<?> inventoryClass;
    public final String foreignKey;
    public final String expandedInventoryKey;
    public final boolean hidden;
    public final Class<?> suppressedInventoryClass;

    public ExpandedQuery(Class<?> inventoryClassToExpand,
                         String expandedField,
                         Class<?> inventoryClass,
                         String foreignKey,
                         String expandedInventoryKey,
                         boolean hidden,
                         Class<?> suppressedInventoryClass) {
        this.inventoryClassToExpand = inventoryClassToExpand;
        this.expandedField = expandedField;
        this.inventoryClass = inventoryClass;
        this.foreignKey = foreignKey;
        this.expandedInventoryKey = expandedInventoryKey;
        this.hidden = hidden;
        this.suppressedInventoryClass = suppressedInventoryClass;
    }
}
