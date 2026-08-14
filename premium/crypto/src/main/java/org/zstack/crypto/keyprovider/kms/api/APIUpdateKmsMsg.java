package org.zstack.crypto.keyprovider.kms.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.api.APIUpdateKeyProviderMsg;
import org.zstack.header.keyprovider.KeyProviderConstant;
import org.zstack.header.keyprovider.KmipVersion;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/key-providers/kms/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateKmsEvent.class,
        isAction = true
)
@OverriddenApiParams({
        @OverriddenApiParam(field = "uuid", param = @APIParam(resourceType = KmsVO.class, emptyString = false)),
})
public class APIUpdateKmsMsg extends APIUpdateKeyProviderMsg implements APIAuditor {
    @APIParam(required = false, maxLength = 255, emptyString = false)
    private String endpoint;

    @APIParam(required = false, numberRange = {1, 65535}, emptyString = false)
    private Integer port;

    @APIParam(required = false, maxLength = 32, emptyString = false, validEnums = {KmipVersion.class})
    private String kmipVersion;

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
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIUpdateKmsMsg) msg).getUuid(), KmsVO.class);
    }

    public static APIUpdateKmsMsg __example__() {
        APIUpdateKmsMsg msg = new APIUpdateKmsMsg();
        msg.setUuid(uuid(KmsVO.class));
        msg.setDescription("example");
        msg.setEndpoint("kms.example.com");
        msg.setPort(5696);
        msg.setKmipVersion(KeyProviderConstant.DEFAULT_KMIP_VERSION);
        msg.setUsername("user");
        msg.setPassword("password");
        return msg;
    }
}
