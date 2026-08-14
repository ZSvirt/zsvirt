package org.zstack.header.query;

import org.zstack.header.search.Inventory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;

/**
 */
public class ExpandedQueryStruct {
    private Class inventoryClassToExpand;
    private String expandedField;
    private Class inventoryClass;
    private String foreignKey;
    private String expandedInventoryKey;
    private Class suppressedInventoryClass;
    private boolean hidden;

    public Class getSuppressedInventoryClass() {
        return suppressedInventoryClass;
    }

    public void setSuppressedInventoryClass(Class suppressedInventoryClass) {
        this.suppressedInventoryClass = suppressedInventoryClass;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getExpandedField() {
        return expandedField;
    }

    public void setExpandedField(String expandedField) {
        this.expandedField = expandedField;
    }

    public Class getInventoryClass() {
        return inventoryClass;
    }

    public void setInventoryClass(Class inventoryClass) {
        this.inventoryClass = inventoryClass;
    }

    public String getForeignKey() {
        return foreignKey;
    }

    public void setForeignKey(String foreignKey) {
        this.foreignKey = foreignKey;
    }

    public String getExpandedInventoryKey() {
        return expandedInventoryKey;
    }

    public void setExpandedInventoryKey(String expandedInventoryKey) {
        this.expandedInventoryKey = expandedInventoryKey;
    }

    public Class getInventoryClassToExpand() {
        return inventoryClassToExpand;
    }

    public void setInventoryClassToExpand(Class inventoryClassToExpand) {
        this.inventoryClassToExpand = inventoryClassToExpand;
    }

    public static ExpandedQueryStruct fromExpandedQueryAnnotation(Class inventoryClassToExpand, ExpandedQuery at) {
        ExpandedQueryStruct s = new ExpandedQueryStruct();
        s.inventoryClassToExpand = inventoryClassToExpand;
        s.inventoryClass = at.inventoryClass();
        s.foreignKey = at.foreignKey();
        s.expandedField = at.expandedField();
        s.expandedInventoryKey = at.expandedInventoryKey();
        s.hidden = at.hidden();
        return s;
    }

    public void check() {
        DebugUtils.Assert(inventoryClass.isAnnotationPresent(Inventory.class),
                String.format("Inventory class[%s] claims class[%s] as its expanded query class; However, class[%s] is not annotated by @Inventory",
                        inventoryClassToExpand.getName(), inventoryClass.getName(), inventoryClass.getName())
        );

        DebugUtils.Assert(FieldUtils.hasField(foreignKey, inventoryClassToExpand),
                String.format("Inventory class[%s] doesn't have field[%s] that is claimed as foreign key for expanded class",
                        inventoryClassToExpand.getName(), foreignKey)
        );

        DebugUtils.Assert(FieldUtils.hasField(expandedInventoryKey, inventoryClass),
                String.format("Expanded inventory class[%s] doesn't have field[%s]", inventoryClass.getName(), expandedInventoryKey)
        );
    }
}
