package org.zstack.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l3.L3NetworkMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mingjian.deng on 2018/6/27.
 */
public class AttachNetworkServiceToL3Msg extends NeedReplyMessage implements L3NetworkMessage {
    private String l3NetworkUuid;
    private Map<String, List<String>> networkServices;

    private List<String> apiSystemTags = new ArrayList<>();

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public Map<String, List<String>> getNetworkServices() {
        return networkServices;
    }

    public void setNetworkServices(Map<String, List<String>> networkServices) {
        this.networkServices = networkServices;
    }

    public List<String> getApiSystemTags() {
        return apiSystemTags;
    }

    public void setApiSystemTags(List<String> apiSystemTags) {
        this.apiSystemTags = apiSystemTags;
    }
}
