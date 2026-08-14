package org.zstack.sdk.identity.ldap.entity;



public class LdapEntryInventory  {

    public java.lang.String dn;
    public void setDn(java.lang.String dn) {
        this.dn = dn;
    }
    public java.lang.String getDn() {
        return this.dn;
    }

    public boolean enable;
    public void setEnable(boolean enable) {
        this.enable = enable;
    }
    public boolean getEnable() {
        return this.enable;
    }

    public java.util.List attributes;
    public void setAttributes(java.util.List attributes) {
        this.attributes = attributes;
    }
    public java.util.List getAttributes() {
        return this.attributes;
    }

}
