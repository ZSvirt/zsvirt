package org.zstack.network.service.flat;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by miao on 16-7-19.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetL3NetworkDhcpIpAddressReply extends APIReply {
    private String ip;
    private String ip6;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp6() {
        return ip6;
    }

    public void setIp6(String ip6) {
        this.ip6 = ip6;
    }

    public static APIGetL3NetworkDhcpIpAddressReply __example__() {
        APIGetL3NetworkDhcpIpAddressReply reply = new APIGetL3NetworkDhcpIpAddressReply();

        reply.setIp("192.168.100.3");

        return reply;
    }

}
