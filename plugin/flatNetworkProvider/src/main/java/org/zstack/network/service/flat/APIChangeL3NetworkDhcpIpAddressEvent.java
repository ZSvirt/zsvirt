package org.zstack.network.service.flat;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIChangeL3NetworkDhcpIpAddressEvent extends APIEvent {
    private String dhcpServerIp;
    private String dhcpv6ServerIp;

    public String getDhcpServerIp() {
        return dhcpServerIp;
    }

    public void setDhcpServerIp(String dhcpServerIp) {
        this.dhcpServerIp = dhcpServerIp;
    }

    public String getDhcpv6ServerIp() {
        return dhcpv6ServerIp;
    }

    public void setDhcpv6ServerIp(String dhcpv6ServerIp) {
        this.dhcpv6ServerIp = dhcpv6ServerIp;
    }

    public APIChangeL3NetworkDhcpIpAddressEvent() {
    }

    public APIChangeL3NetworkDhcpIpAddressEvent(String apiId) {
        super(apiId);
    }

    public static APIChangeL3NetworkDhcpIpAddressEvent __example__() {
        APIChangeL3NetworkDhcpIpAddressEvent reply = new APIChangeL3NetworkDhcpIpAddressEvent();

        reply.setDhcpServerIp("192.168.100.3");
        reply.setDhcpv6ServerIp("2024:04:28:01::100");

        return reply;
    }

}
