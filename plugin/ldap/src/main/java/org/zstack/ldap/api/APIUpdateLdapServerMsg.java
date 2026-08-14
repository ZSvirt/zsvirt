package org.zstack.ldap.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.ldap.entity.LdapEncryptionType;
import org.zstack.ldap.entity.LdapServerType;
import org.zstack.ldap.entity.LdapServerVO;

@RestRequest(
        path = "/ldap/servers/{ldapServerUuid}",
        method = HttpMethod.PUT,
        responseClass = APIUpdateLdapServerEvent.class,
        isAction = true
)
public class APIUpdateLdapServerMsg extends APIMessage {
    @APIParam(maxLength = 32, resourceType = LdapServerVO.class)
    private String ldapServerUuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 1024, required = false)
    private String url;

    @APIParam(maxLength = 1024, required = false)
    private String base;

    @APIParam(maxLength = 1024, required = false)
    private String username;

    @APIParam(maxLength = 1024, required = false, password = true)
    @NoLogging
    private String password;

    @APIParam(maxLength = 1024, validEnums = {LdapEncryptionType.class}, required = false)
    private String encryption;

    @APIParam(validEnums = {LdapServerType.class}, required = false)
    private String serverType;

    @APIParam(maxLength = 255, required = false)
    private String usernameProperty;

    @APIParam(maxLength = 2048, required = false)
    private String filter;

    @APIParam(validEnums = {SyncCreatedAccountStrategy.class}, required = false)
    private String syncCreatedAccountStrategy;

    @APIParam(validEnums = {SyncDeletedAccountStrategy.class}, required = false)
    private String syncDeletedAccountStrategy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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

    public String getLdapServerUuid() {
        return ldapServerUuid;
    }

    public void setLdapServerUuid(String ldapServerUuid) {
        this.ldapServerUuid = ldapServerUuid;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getSyncCreatedAccountStrategy() {
        return syncCreatedAccountStrategy;
    }

    public void setSyncCreatedAccountStrategy(String syncCreatedAccountStrategy) {
        this.syncCreatedAccountStrategy = syncCreatedAccountStrategy;
    }

    public String getSyncDeletedAccountStrategy() {
        return syncDeletedAccountStrategy;
    }

    public void setSyncDeletedAccountStrategy(String syncDeletedAccountStrategy) {
        this.syncDeletedAccountStrategy = syncDeletedAccountStrategy;
    }

    public static APIUpdateLdapServerMsg __example__() {
        APIUpdateLdapServerMsg msg = new APIUpdateLdapServerMsg();
        msg.setName("new name");
        msg.setLdapServerUuid(uuid());

        return msg;
    }

}
