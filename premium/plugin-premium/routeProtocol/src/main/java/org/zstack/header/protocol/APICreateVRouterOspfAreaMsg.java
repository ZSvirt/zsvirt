package org.zstack.header.protocol;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/routerArea",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateVRouterOspfAreaEvent.class
)
public class APICreateVRouterOspfAreaMsg extends APICreateMessage implements APIAuditor {

    /**
     * @desc max length of 64 characters
     * @required
     */
    @APIParam(maxLength = 64)
    private String areaId;

    @APIParam(required = false, maxLength = 16)
    private String areaAuth;

    @APIParam(required = false, maxLength = 16)
    private String areaType;

    @APIParam(required = false, maxLength = 16, password = true)
    @NoLogging
    private String password;

    @APIParam(required = false, numberRange = {1L, 255L})
    private Integer keyId;

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaAuth() {
        return areaAuth;
    }

    public void setAreaAuth(String areaAuth) {
        this.areaAuth = areaAuth;
    }

    public String getAreaType() {
        return areaType;
    }

    public void setAreaType(String areaType) {
        this.areaType = areaType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getKeyId() {
        return keyId;
    }

    public void setKeyId(Integer keyId) {
        this.keyId = keyId;
    }

    public static APICreateVRouterOspfAreaMsg __example__() {
        APICreateVRouterOspfAreaMsg msg = new APICreateVRouterOspfAreaMsg();
        msg.setAreaId("1.1.1.1");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVRouterOspfAreaEvent)rsp).getInventory().getUuid() : "", RouterAreaVO.class);
    }
}
