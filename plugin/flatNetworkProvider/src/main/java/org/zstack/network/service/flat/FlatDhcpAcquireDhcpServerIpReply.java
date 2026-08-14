package org.zstack.network.service.flat;

import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.UsedIpInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 10/11/2015.
 */
public class FlatDhcpAcquireDhcpServerIpReply extends MessageReply {
    public static class DhcpServerIpStruct {
        private String ip;
        private String netmask;
        private String usedIpUuid;
        private UsedIpInventory usedIp;
        private IpRangeInventory ipr;
        private int ipVersion;

        public String getNetmask() {
            return netmask;
        }

        public void setNetmask(String netmask) {
            this.netmask = netmask;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getUsedIpUuid() {
            return usedIpUuid;
        }

        public void setUsedIpUuid(String usedIpUuid) {
            this.usedIpUuid = usedIpUuid;
        }

        public UsedIpInventory getUsedIp() {
            return usedIp;
        }

        public void setUsedIp(UsedIpInventory usedIp) {
            this.usedIp = usedIp;
        }

        public IpRangeInventory getIpr() {
            return ipr;
        }

        public void setIpr(IpRangeInventory ipr) {
            this.ipr = ipr;
        }

        public int getIpVersion() {
            return ipVersion;
        }

        public void setIpVersion(int ipVersion) {
            this.ipVersion = ipVersion;
        }
    }

    List<DhcpServerIpStruct> dhcpServerList = new ArrayList<>();

    public List<DhcpServerIpStruct> getDhcpServerList() {
        return dhcpServerList;
    }

    public void setDhcpServerList(List<DhcpServerIpStruct> dhcpServerList) {
        this.dhcpServerList = dhcpServerList;
    }
}
