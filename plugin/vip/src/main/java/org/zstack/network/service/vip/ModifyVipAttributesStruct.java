package org.zstack.network.service.vip;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/11/30.
 */
public class ModifyVipAttributesStruct {
    private String serviceProvider;
    private boolean isServiceProvider;
    private String useFor;
    private String serviceUuid;
    private boolean isUserFor;
    private List<String> peerL3NetworkUuids;
    private boolean isPeerL3NetworkUuid;

    public void clearServiceProvider() {
        serviceProvider = null;
        isServiceProvider = false;
    }

    public void clearUseFor() {
        useFor = null;
        isUserFor = false;
    }

    public void clearPeerL3NetworkUuid() {
        peerL3NetworkUuids = null;
        isPeerL3NetworkUuid = false;
    }

    public boolean isServiceProvider() {
        return isServiceProvider;
    }

    public boolean isUserFor() {
        return isUserFor;
    }

    public boolean isPeerL3NetworkUuid() {
        return isPeerL3NetworkUuid;
    }


    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
        isServiceProvider = true;
    }

    public String getUseFor() {
        return useFor;
    }

    public void setUseFor(String useFor) {
        this.useFor = useFor;
        isUserFor = true;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public List<String> getPeerL3NetworkUuids() {
        return peerL3NetworkUuids;
    }

    public void setPeerL3NetworkUuid(String peerL3NetworkUuid) {
        this.peerL3NetworkUuids = new ArrayList<>();
        this.peerL3NetworkUuids.add(peerL3NetworkUuid);
        isPeerL3NetworkUuid = true;
    }

    public void setPeerL3NetworkUuids(List<String> peerL3NetworkUuids) {
        this.peerL3NetworkUuids = peerL3NetworkUuids;
        isPeerL3NetworkUuid = true;
    }
}
