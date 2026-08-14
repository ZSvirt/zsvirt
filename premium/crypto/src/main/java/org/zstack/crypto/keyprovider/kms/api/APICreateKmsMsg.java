package org.zstack.crypto.keyprovider.kms.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.api.APICreateKeyProviderMsg;
import org.zstack.header.keyprovider.KeyProviderConstant;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KmipVersion;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@TagResourceType(KeyProviderVO.class)
@RestRequest(
        path = "/key-providers/kms",
        method = HttpMethod.POST,
        responseClass = APICreateKmsEvent.class,
        parameterName = "params"
)
public class APICreateKmsMsg extends APICreateKeyProviderMsg implements APIAuditor {
    @APIParam(maxLength = 255, emptyString = false)
    private String endpoint;

    @APIParam(numberRange = {1, 65535}, emptyString = false)
    private Integer port;

    @APIParam(required = false, maxLength = 32, emptyString = false, validEnums = {KmipVersion.class})
    private String kmipVersion = KeyProviderConstant.DEFAULT_KMIP_VERSION;

    @APIParam(required = false, maxLength = 255, emptyString = false)
    private String username;

    @APIParam(required = false, maxLength = 255, emptyString = false)
    @NoLogging
    private String password;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getKmipVersion() {
        return kmipVersion;
    }

    public void setKmipVersion(String kmipVersion) {
        this.kmipVersion = kmipVersion;
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

    @Override
    public String getType() {
        return KeyProviderType.KMS.name();
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APICreateKmsEvent evt = (APICreateKmsEvent) rsp;
            if (evt.getInventory() != null) {
                uuid = evt.getInventory().getUuid();
            }
        }

        return new Result(uuid, KeyProviderVO.class);
    }

    public static APICreateKmsMsg __example__() {
        APICreateKmsMsg msg = new APICreateKmsMsg();
        msg.setName("kp-kms");
        msg.setDescription("example");
        msg.setEndpoint("kms.example.com");
        msg.setPort(5696);
        msg.setKmipVersion(KeyProviderConstant.DEFAULT_KMIP_VERSION);
        msg.setUsername("user");
        msg.setPassword("password");
        return msg;
    }
}
