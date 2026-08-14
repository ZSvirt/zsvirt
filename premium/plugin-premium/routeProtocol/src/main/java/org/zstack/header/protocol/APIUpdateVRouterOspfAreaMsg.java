package org.zstack.header.protocol;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/routerArea/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateVRouterOspfAreaEvent.class,
        isAction = true
)
public class APIUpdateVRouterOspfAreaMsg extends APIMessage implements AreaMessage {
    @APIParam(resourceType = RouterAreaVO.class)
    private String uuid;

    @APIParam(required = false, maxLength = 16)
    private String areaAuth;

    @APIParam(required = false, maxLength = 16)
    private String areaType;

    @APIParam(required = false, maxLength = 16, password = true)
    @NoLogging
    private String password;

    @APIParam(required = false, numberRange = {1L, 255L})
    private Integer keyId;


    public static APIUpdateVRouterOspfAreaMsg __example__() {
        APIUpdateVRouterOspfAreaMsg msg = new APIUpdateVRouterOspfAreaMsg();
        msg.setUuid(uuid());
        msg.setAreaType(RouterAreaType.Standard.toString());

        return msg;
    }

    @Override
    public String getAreaUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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
}
