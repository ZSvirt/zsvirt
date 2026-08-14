package org.zstack.ldap.header;

import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.ldap.entity.LdapEncryptionType;
import org.zstack.ldap.entity.LdapServerType;

public class LdapAccountSourceSpec extends AbstractAccountSourceSpec {
    private String url;
    private String baseDn;
    private String logInUserName;
    private String logInPassword;
    private String serverName;
    private String filter;
    private LdapEncryptionType encryption;
    private LdapServerType serverType;
    private SyncCreatedAccountStrategy createAccountStrategy;
    private SyncDeletedAccountStrategy deleteAccountStrategy;

    /**
     * Which field in LDAP server does the user use as the username for logging in?
     */
    private String usernameProperty;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getLogInUserName() {
        return logInUserName;
    }

    public void setLogInUserName(String logInUserName) {
        this.logInUserName = logInUserName;
    }

    public String getLogInPassword() {
        return logInPassword;
    }

    public void setLogInPassword(String logInPassword) {
        this.logInPassword = logInPassword;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public LdapEncryptionType getEncryption() {
        return encryption;
    }

    public void setEncryption(LdapEncryptionType encryption) {
        this.encryption = encryption;
    }

    public LdapServerType getServerType() {
        return serverType;
    }

    public void setServerType(LdapServerType serverType) {
        this.serverType = serverType;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }

    public SyncCreatedAccountStrategy getCreateAccountStrategy() {
        return createAccountStrategy;
    }

    public void setCreateAccountStrategy(SyncCreatedAccountStrategy createAccountStrategy) {
        this.createAccountStrategy = createAccountStrategy;
    }

    public SyncDeletedAccountStrategy getDeleteAccountStrategy() {
        return deleteAccountStrategy;
    }

    public void setDeleteAccountStrategy(SyncDeletedAccountStrategy deleteAccountStrategy) {
        this.deleteAccountStrategy = deleteAccountStrategy;
    }
}
