package org.zstack.ldap.entity;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;

import javax.persistence.*;

@Entity
@Table
@AutoDeleteTag
public class LdapServerVO extends ThirdPartyAccountSourceVO {
    @Column
    private String url;

    @Column
    private String base;

    @Column
    private String username;

    @Column
    private String password;

    @Column
    private String encryption;

    @Column
    @Enumerated(EnumType.STRING)
    private LdapServerType serverType;

    @Column
    private String filter;

    @Column
    private String usernameProperty;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
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
}
