package org.zstack.ipsec;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * created by boce.wang 07/09/2022
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetVpcIPsecLogReply extends APIReply {
    private String ipsecLog;

    public String getIpsecLog() {
        return ipsecLog;
    }

    public void setIpsecLog(String ipsecLog) {
        this.ipsecLog = ipsecLog;
    }

    public static APIGetVpcIPsecLogReply _example__() {
        APIGetVpcIPsecLogReply reply = new APIGetVpcIPsecLogReply();
        reply.setIpsecLog("ipsec charon log");
        return reply;
    }
}
