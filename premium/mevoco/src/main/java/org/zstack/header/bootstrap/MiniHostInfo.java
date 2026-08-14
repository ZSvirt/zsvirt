package org.zstack.header.bootstrap;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.SDK;

import java.util.List;

@PythonClassInventory
@SDK(sdkClassName = "MiniHostInfo")
public class MiniHostInfo {
    private String sn;

    private List<String> dnsAddresses;

    @APIParam(required = false)
    private MiniNetworkConfigStruct ipmi;

    private MiniNetworkConfigStruct mgmt;

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public MiniNetworkConfigStruct getIpmi() {
        return ipmi;
    }

    public void setIpmi(MiniNetworkConfigStruct ipmi) {
        this.ipmi = ipmi;
    }

    public MiniNetworkConfigStruct getMgmt() {
        return mgmt;
    }

    public void setMgmt(MiniNetworkConfigStruct mgmt) {
        this.mgmt = mgmt;
    }

    public List<String> getDnsAddresses() {
        return dnsAddresses;
    }

    public void setDnsAddresses(List<String> dnsAddresses) {
        this.dnsAddresses = dnsAddresses;
    }
}
