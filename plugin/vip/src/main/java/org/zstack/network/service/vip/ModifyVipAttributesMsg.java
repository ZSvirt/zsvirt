package org.zstack.network.service.vip;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/11/30.
 */
public class ModifyVipAttributesMsg extends NeedReplyMessage implements VipMessage {
    private String vipUuid;
    private ModifyVipAttributesStruct struct;


    public ModifyVipAttributesMsg() {
        struct = new ModifyVipAttributesStruct();
    }

    public ModifyVipAttributesStruct getStruct() {
        return struct;
    }

    public void setStruct(ModifyVipAttributesStruct struct) {
        this.struct = struct;
    }

    public void clearServiceProvider() {
        struct.clearServiceProvider();
    }

    public void clearUseFor() {
        struct.clearUseFor();
    }

    public void clearPeerL3NetworkUuid() {
        struct.clearPeerL3NetworkUuid();
    }

    public boolean isServiceProvider() {
        return struct.isServiceProvider();
    }

    public boolean isUserFor() {
        return struct.isUserFor();
    }

    public boolean isPeerL3NetworkUuid() {
        return struct.isPeerL3NetworkUuid();
    }

    public String getServiceProvider() {
        return struct.getServiceProvider();
    }

    public void setServiceProvider(String serviceProvider) {
        struct.setServiceProvider(serviceProvider);
    }

    public String getUseFor() {
        return struct.getUseFor();
    }

    public void setUseFor(String useFor) {
        struct.setUseFor(useFor);
    }

    @Override
    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

}
