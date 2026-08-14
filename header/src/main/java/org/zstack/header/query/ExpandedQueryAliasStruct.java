package org.zstack.header.query;

/**
 */
public class ExpandedQueryAliasStruct {
    private String expandedField;
    private String alias;
    private Class inventoryClass;

    public String getExpandedField() {
        return expandedField;
    }

    public void setExpandedField(String expandedField) {
        this.expandedField = expandedField;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Class getInventoryClass() {
        return inventoryClass;
    }

    public void setInventoryClass(Class inventoryClass) {
        this.inventoryClass = inventoryClass;
    }
}
