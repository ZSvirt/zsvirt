package org.zstack.ldap.entity;

import org.zstack.header.configuration.PythonClassInventory;

import java.util.List;

@PythonClassInventory
public class LdapEntryAttributeInventory {
    private String id;
    private List<Object> values;
    private boolean orderMatters;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    public boolean isOrderMatters() {
        return orderMatters;
    }

    public void setOrderMatters(boolean orderMatters) {
        this.orderMatters = orderMatters;
    }
}
