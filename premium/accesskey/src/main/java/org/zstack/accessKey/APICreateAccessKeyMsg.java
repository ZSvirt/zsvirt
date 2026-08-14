package org.zstack.accessKey;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/accesskeys",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateAccessKeyEvent.class
)
public class APICreateAccessKeyMsg extends APICreateMessage implements APIAuditor, CreateAccessKey {
    @APIParam(maxLength = 32, resourceType = AccountVO.class)
    private String accountUuid;
    @Deprecated
    @APIParam(maxLength = 32, required = false)
    private String userUuid;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(minLength = AccessKeyConstant.ACCESSKEY_ID_MIN_LEN, maxLength = AccessKeyConstant.ACCESSKEY_ID_LEN, required = false)
    private String AccessKeyID;
    @APIParam(minLength = AccessKeyConstant.ACCESSKEY_SECRET_MIN_LEN, maxLength = AccessKeyConstant.ACCESSKEY_SECRET_LEN, required = false)
    private String AccessKeySecret;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @Deprecated
    public String getUserUuid() {
        return userUuid;
    }

    @Deprecated
    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getAccessKeyID() {
        return AccessKeyID;
    }

    public void setAccessKeyID(String accessKeyID) {
        this.AccessKeyID = accessKeyID;
    }

    public String getAccessKeySecret() {
        return AccessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.AccessKeySecret = accessKeySecret;
    }

    public static APICreateAccessKeyMsg __example__() {
        APICreateAccessKeyMsg msg = new APICreateAccessKeyMsg();
        msg.setAccountUuid("db517023502d34ef9309e49674af88d0");
        return msg;
    }

    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APICreateAccessKeyEvent evt = (APICreateAccessKeyEvent) rsp;
            uuid = evt.getInventory().getUuid();
        }
        return new Result(uuid, AccessKeyVO.class);
    }
}
