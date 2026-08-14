package org.zstack.header.baremetal.pxeserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.zone.ZoneEO;

/**
 * Created by GuoYi on 2017/3/26.
 */
@TagResourceType(BaremetalPxeServerVO.class)
@RestRequest(
        path = "/baremetal/pxeservers",
        method = HttpMethod.POST,
        responseClass = APICreateBaremetalPxeServerEvent.class,
        parameterName = "params"
)
public class APICreateBaremetalPxeServerMsg extends APICreateMessage implements APIAuditor {
    @APIParam(resourceType = ZoneEO.class)
    private String zoneUuid;
    @APIParam(maxLength = 255, emptyString = false)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(maxLength = 255)
    private String hostname;
    @APIParam(maxLength = 255)
    private String sshUsername;
    @APIParam(maxLength = 255)
    @NoLogging
    private String sshPassword;
    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer sshPort = 22;
    @APIParam(maxLength = 2048)
    private String storagePath;
    @APIParam(maxLength = 128, emptyString = false)
    private String dhcpInterface;
    @APINoSee
    private String dhcpInterfaceAddress;

    // auto detect dhcp range if not given
    @APIParam(required = false)
    private String dhcpRangeBegin;
    @APIParam(required = false)
    private String dhcpRangeEnd;
    @APIParam(required = false)
    private String dhcpRangeNetmask;

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getDhcpInterface() {
        return dhcpInterface;
    }

    public void setDhcpInterface(String dhcpInterface) {
        this.dhcpInterface = dhcpInterface;
    }

    public String getDhcpInterfaceAddress() {
        return dhcpInterfaceAddress;
    }

    public void setDhcpInterfaceAddress(String dhcpInterfaceAddress) {
        this.dhcpInterfaceAddress = dhcpInterfaceAddress;
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

    public static APICreateBaremetalPxeServerMsg __example__() {
        APICreateBaremetalPxeServerMsg msg = new APICreateBaremetalPxeServerMsg();
        msg.setZoneUuid(uuid());
        msg.setName("test");
        msg.setHostname("127.0.0.1");
        msg.setSshUsername("root");
        msg.setSshPassword("password");
        msg.setStoragePath("/zstack_bm_cache");
        msg.setDhcpInterface("eth0");
        msg.setDhcpRangeBegin("10.0.0.1");
        msg.setDhcpRangeEnd("10.0.0.255");
        msg.setDhcpRangeNetmask("255.255.255.0");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateBaremetalPxeServerEvent)rsp).getInventory().getUuid() : "", BaremetalPxeServerVO.class);
    }
}
