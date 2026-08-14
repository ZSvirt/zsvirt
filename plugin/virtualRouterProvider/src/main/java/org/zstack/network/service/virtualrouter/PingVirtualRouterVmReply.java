package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.MessageReply;

import java.util.HashMap;
import java.util.List;

/**
 * Created by frank on 6/29/2015.
 */
public class PingVirtualRouterVmReply extends MessageReply {
    private boolean connected;
    private boolean doReconnect;
    private String haStatus;
    private String vrUuid;
    private HashMap<String, HashMap<String, String>> serviceHealthList;


    public PingVirtualRouterVmReply(String vrUuid) {
        this.vrUuid = vrUuid;
    }

    public boolean isDoReconnect() {
        return doReconnect;
    }

    public void setDoReconnect(boolean doReconnect) {
        this.doReconnect = doReconnect;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getHaStatus() {
        return haStatus;
    }

    public void setHaStatus(String haStatus) {
        this.haStatus = haStatus;
    }

    public String getVrUuid() {
        return vrUuid;
    }

    public void setVrUuid(String vrUuid) {
        this.vrUuid = vrUuid;
    }

    public HashMap<String, HashMap<String, String>> getServiceHealthList() {
        return serviceHealthList;
    }

    public void setServiceHealthList(HashMap<String, HashMap<String, String>> serviceHealthList) {
        this.serviceHealthList = serviceHealthList;
    }
}
