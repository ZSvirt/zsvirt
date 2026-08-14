package org.zstack.header.vipQos;

import java.io.Serializable;

/**
 * Created by liangbo.zhou on 17-6-16.
 */
public class VipQosStruct implements Serializable {
    private String      vipUuid;
    private String      vip;
    private String      publicNic;
    private int         port;
    private long        inboundBandwidth;
    private long        outboundBandwidth;
    private String      type;
    private String      l3NetworkUuid;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getVip() {
        return vip;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public void setInboundBandwidth(long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }

    public long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public void setOutboundBandwidth(long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getPublicNic() {
        return publicNic;
    }

    public void setPublicNic(String publicNic) {
        this.publicNic = publicNic;
    }
}
