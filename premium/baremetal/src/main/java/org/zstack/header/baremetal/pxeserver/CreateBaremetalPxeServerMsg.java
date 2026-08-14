package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2017/5/7.
 */
public class CreateBaremetalPxeServerMsg extends NeedReplyMessage {
    private String name;
    private String description;
    private String dhcpInterface;
    private String dhcpRangeBegin;
    private String dhcpRangeEnd;
    private String dhcpRangeNetmask;
    private String accountUuid;

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

    public String getDhcpInterface() {
        return dhcpInterface;
    }

    public void setDhcpInterface(String dhcpInterface) {
        this.dhcpInterface = dhcpInterface;
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

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
