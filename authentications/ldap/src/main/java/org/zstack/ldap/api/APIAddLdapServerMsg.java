package org.zstack.ldap.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.ldap.LdapConstant;
import org.zstack.ldap.entity.LdapEncryptionType;
import org.zstack.ldap.entity.LdapServerType;
import org.zstack.ldap.entity.LdapServerVO;

@RestRequest(
        path = "/ldap/servers",
        method = HttpMethod.POST,
        responseClass = APIAddLdapServerEvent.class,
        parameterName = "params"
)
public class APIAddLdapServerMsg extends APIMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 1024)
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;

    @APIParam(maxLength = 1024)
    private String base;

    @APIParam(maxLength = 1024)
    private String username;

    @APIParam(maxLength = 1024)
    @NoLogging
    private String password;

    @APIParam(maxLength = 1024, validEnums = {LdapEncryptionType.class})
    private String encryption;

    @APIParam(validEnums = {LdapServerType.class}, required = false)
    private String serverType = "Unknown";

    @APIParam(maxLength = 255, required = false)
    private String usernameProperty = LdapConstant.LDAP_UID_KEY;

    @APIParam(maxLength = 2048, required = false)
    private String filter = LdapConstant.DEFAULT_PERSON_FILTER;

    @APIParam(validEnums = {SyncCreatedAccountStrategy.class}, required = false)
    private String syncCreatedAccountStrategy = "CreateAccount";

    @APIParam(validEnums = {SyncDeletedAccountStrategy.class}, required = false)
    private String syncDeletedAccountStrategy = "StaleAccount";

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

    public static APIAddLdapServerMsg __example__() {
        APIAddLdapServerMsg msg = new APIAddLdapServerMsg();
        msg.setName("miao");
        msg.setDescription("miao desc");
        msg.setUrl("ldap://localhost:1888");
        msg.setBase("dc=example,dc=com");
        msg.setUsername("");
        msg.setPassword("");
        msg.setEncryption("None");
        msg.setServerType("WindowsAD");
        msg.setFilter("(cn=Micha Kops)");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddLdapServerEvent)rsp).getInventory().getUuid() : "", LdapServerVO.class);
    }
}
