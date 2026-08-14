package org.zstack.network.securitygroup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIValidateSecurityGroupRuleReply extends APIReply {
    private boolean available;
    private String code;
    private String reason;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public static APIValidateSecurityGroupRuleReply __example__() {
        APIValidateSecurityGroupRuleReply reply = new APIValidateSecurityGroupRuleReply();
        reply.setAvailable(true);
        reply.setCode(SecurityGroupErrors.RULE_CHECK_OK.toString());
        return reply;
    }
}
