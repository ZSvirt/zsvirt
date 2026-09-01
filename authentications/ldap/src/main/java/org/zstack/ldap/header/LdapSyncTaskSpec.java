package org.zstack.ldap.header;

import org.zstack.identity.imports.header.SyncTaskSpec;
import org.zstack.ldap.entity.LdapServerType;

public class LdapSyncTaskSpec extends SyncTaskSpec {
    private String filter;
    private int maxAccountCount;
    private String usernameProperty;
    private LdapServerType serverType;

    public LdapSyncTaskSpec() {
    }

    public LdapSyncTaskSpec(SyncTaskSpec spec) {
        this.setSourceUuid(spec.getSourceUuid());
        this.setSourceType(spec.getSourceType());
        this.setCreateAccountStrategy(spec.getCreateAccountStrategy());
        this.setDeleteAccountStrategy(spec.getDeleteAccountStrategy());
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public int getMaxAccountCount() {
        return maxAccountCount;
    }

    public void setMaxAccountCount(int maxAccountCount) {
        this.maxAccountCount = maxAccountCount;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }

    public LdapServerType getServerType() {
        return serverType;
    }

    public void setServerType(LdapServerType serverType) {
        this.serverType = serverType;
    }
}
