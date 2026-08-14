package org.zstack.header.baremetal.pxeserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2017/5/7.
 *
 * Do not update dhcpInterface of pxeserver since there are bm instance trying to connect to it
 */
@RestRequest(
        path = "/baremetal/pxeservers/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateBaremetalPxeServerEvent.class
)
public class APIUpdateBaremetalPxeServerMsg extends APIMessage {
    @APIParam(resourceType = BaremetalPxeServerVO.class)
    private String uuid;
    @APIParam(required = false, maxLength = 255, emptyString = false)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(required = false)
    private String dhcpRangeBegin;
    @APIParam(required = false)
    private String dhcpRangeEnd;
    @APIParam(required = false)
    private String dhcpRangeNetmask;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public String getDhcpRangeBegin() {
        return dhcpRangeBegin;
    }

    public void setDhcpRangeBegin(String dhcpRangeBegin) {
        this.dhcpRangeBegin = dhcpRangeBegin;
    }

    public String getDhcpRangeEnd() {
        return dhcpRangeEnd;
    }

    public void setDhcpRangeEnd(String dhcpRangeEnd) {
        this.dhcpRangeEnd = dhcpRangeEnd;
    }

    public String getDhcpRangeNetmask() {
        return dhcpRangeNetmask;
    }

    public void setDhcpRangeNetmask(String dhcpRangeNetmask) {
        this.dhcpRangeNetmask = dhcpRangeNetmask;
    }

    public static APIUpdateBaremetalPxeServerMsg __example__() {
        APIUpdateBaremetalPxeServerMsg msg = new APIUpdateBaremetalPxeServerMsg();
        msg.setName("test");
        msg.setUuid(uuid());
        msg.setDhcpRangeBegin("10.0.0.2");
        msg.setDhcpRangeBegin("10.0.0.200");
        msg.setDhcpRangeNetmask("255.255.255.0");
        return msg;
    }
}