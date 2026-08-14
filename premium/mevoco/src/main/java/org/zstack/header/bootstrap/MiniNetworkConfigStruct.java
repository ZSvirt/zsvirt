package org.zstack.header.bootstrap;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.SDK;

@PythonClassInventory
@SDK(sdkClassName = "MiniNetworkConfigStruct")
public class MiniNetworkConfigStruct {
    private String gw;

    private String ip;

    @APIParam(required = false)
    private String vlan;

    private String bond;

    public String getGw() {
        return gw;
    }

    public void setGw(String gw) {
        this.gw = gw;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getVlan() {
        return vlan;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public String getBond() {
        return bond;
    }

    public void setBond(String bond) {
        this.bond = bond;
    }
}