package org.zstack.ldap.driver;

import java.util.function.Predicate;

public class LdapSearchSpec {
    private String ldapServerUuid;
    private String filter;
    private Integer count;
    private String[] returningAttributes;

    /**
     * If resultFilter is null, do not check and filter results
     */
    private Predicate<String> resultFilter;
    private boolean searchAllAttributes = false;

    public String getLdapServerUuid() {
        return ldapServerUuid;
    }

    public void setLdapServerUuid(String ldapServerUuid) {
        this.ldapServerUuid = ldapServerUuid;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String[] getReturningAttributes() {
        return returningAttributes;
    }

    public void setReturningAttributes(String[] returningAttributes) {
        this.returningAttributes = returningAttributes;
    }

    public Predicate<String> getResultFilter() {
        return resultFilter;
    }

    public void setResultFilter(Predicate<String> resultFilter) {
        this.resultFilter = resultFilter;
    }

    public boolean isSearchAllAttributes() {
        return searchAllAttributes;
    }

    public void setSearchAllAttributes(boolean searchAllAttributes) {
        this.searchAllAttributes = searchAllAttributes;
    }
}
