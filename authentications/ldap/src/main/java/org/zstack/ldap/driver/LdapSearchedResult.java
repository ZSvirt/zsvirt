package org.zstack.ldap.driver;

import org.zstack.ldap.entity.LdapEntryInventory;

import java.util.List;

public class LdapSearchedResult {
    private boolean success = true;
    private List<LdapEntryInventory> result;
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<LdapEntryInventory> getResult() {
        return result;
    }

    public void setResult(List<LdapEntryInventory> result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
