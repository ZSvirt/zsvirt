package org.zstack.sdk.identity.ldap.entity;



public class LdapEntryAttributeInventory  {

    public java.lang.String id;
    public void setId(java.lang.String id) {
        this.id = id;
    }
    public java.lang.String getId() {
        return this.id;
    }

    public java.util.List values;
    public void setValues(java.util.List values) {
        this.values = values;
    }
    public java.util.List getValues() {
        return this.values;
    }

    public boolean orderMatters;
    public void setOrderMatters(boolean orderMatters) {
        this.orderMatters = orderMatters;
    }
    public boolean getOrderMatters() {
        return this.orderMatters;
    }

}
